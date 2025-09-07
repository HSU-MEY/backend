package com.mey.backend.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.dto.ChatResponse;
import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.dto.IntentClassificationResult;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.route.dto.CreateRouteByPlaceIdsRequestDto;
import com.mey.backend.domain.route.dto.RouteCreateResponseDto;
import com.mey.backend.domain.route.service.RouteService;
import com.mey.backend.domain.route.repository.RouteRepository;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final OpenAiApi openAiApi;
    private final RagService ragService;
    private final PlaceRepository placeRepository;
    private final RouteService routeService;
    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자 쿼리를 처리하여 적절한 응답을 반환합니다.
     * 다양한 의도(루트 생성, 기존 루트 검색, 장소 검색, 일반 질문)를 지원합니다.
     */
    public ChatResponse processUserQuery(ChatRequest request) {
        log.info("Processing user query: {}", request.getQuery());
        
        // 1. LLM 기반 의도 분류
        IntentClassificationResult classificationResult = classifyUserIntentWithLLM(request.getQuery());
        log.info("LLM 의도 분류 결과: {} (신뢰도: {}, 근거: {})", 
                classificationResult.getIntent(), 
                classificationResult.getConfidence(), 
                classificationResult.getReasoning());
        
        // 2. 의도별 처리
        return switch (classificationResult.getIntent()) {
            case CREATE_ROUTE -> handleCreateRouteIntent(request);
            case SEARCH_EXISTING_ROUTES -> handleSearchExistingRoutesIntent(request);
            case SEARCH_PLACES -> handleSearchPlacesIntent(request);
            case GENERAL_QUESTION -> handleGeneralQuestionIntent(request);
        };
    }

    private ChatContext extractContextFromQuery(String query, ChatContext existingContext) {
        String systemPrompt = """
            당신은 사용자 질문에서 한류 루트 추천에 필요한 정보를 추출하는 전문가입니다.
            다음 정보를 JSON 형태로 추출해주세요:
            - theme: 테마 (KDRAMA, KPOP, KFOOD, KFASHION 중)
            - region: 지역명 (서울, 부산 등)
            - budget: 예산 (숫자만, 원 단위)
            - preferences: 특별 선호사항
            - durationMinutes: 소요 시간 (분 단위)
            - days: 여행 일수 (1일, 2일, 3일 등)
            
            기존 컨텍스트가 있다면 이를 기반으로 새로운 정보만 업데이트하세요.
            정보가 없으면 null로 설정하세요.
            
            응답 형식:
            {
                "theme": "KPOP",
                "region": "서울",
                "budget": 50000,
                "preferences": null,
                "durationMinutes": null,
                "days": 2
            }
            """;
        
        String contextInfo = existingContext != null ? 
            "기존 컨텍스트: " + convertContextToString(existingContext) : "기존 컨텍스트 없음";
        
        String userMessage = contextInfo + "\n새 질문: " + query;
        
        org.springframework.ai.chat.model.ChatResponse aiResponse = callOpenAi(userMessage, systemPrompt);
        String responseText = aiResponse.getResult().getOutput().getText();
        
        try {
            return objectMapper.readValue(responseText, ChatContext.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse context from AI response: {}", responseText, e);
            return existingContext != null ? existingContext : ChatContext.builder().build();
        }
    }
    
    private String convertContextToString(ChatContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String checkMissingRequiredInfo(ChatContext context) {
        if (context.getTheme() == null) {
            return "어떤 테마의 루트를 찾고 계신가요? (K-POP, K-드라마, K-푸드, K-패션 중 선택해주세요)";
        }
        
        if (context.getRegion() == null || context.getRegion().trim().isEmpty()) {
            return "어느 지역의 루트를 원하시나요? (예: 서울, 부산)";
        }
        
        if (context.getDays() == null || context.getDays() <= 0) {
            return "몇 일 여행을 계획하고 계신가요? (예: 1일, 2일, 3일)";
        }
        
        return null; // 필수 정보가 모두 있음
    }

    private ChatResponse recommendRouteWithRag(ChatContext context, String originalQuery) {
        // 1. 일수 조정 및 컨텍스트 준비
        DaysAdjustmentResult adjustmentResult = adjustDaysIfNeeded(context);
        
        // 2. 장소 검색 및 추출
        List<Long> placeIds = searchAndExtractPlaceIds(adjustmentResult.adjustedContext(), originalQuery);
        if (placeIds == null) {
            return createErrorResponse("죄송합니다. 요청하신 조건에 맞는 장소를 찾을 수 없습니다. 다른 테마나 지역을 시도해보시겠어요?", adjustmentResult.adjustedContext());
        }
        
        // 3. 루트 생성 및 응답
        return createRouteAndResponse(adjustmentResult, placeIds);
    }
    
    private String buildSearchQuery(ChatContext context, String originalQuery) {
        StringBuilder searchQuery = new StringBuilder(originalQuery);
        
        if (context.getTheme() != null) {
            searchQuery.append(" ").append(context.getTheme().name());
        }
        
        if (context.getRegion() != null) {
            searchQuery.append(" ").append(context.getRegion());
        }
        
        if (context.getBudget() != null) {
            searchQuery.append(" 예산 ").append(context.getBudget()).append("원");
        }
        
        return searchQuery.toString();
    }

    private org.springframework.ai.chat.model.ChatResponse callOpenAi(String userInput, String systemMessage) {
        List<Message> messages = Arrays.asList(
                new SystemMessage(systemMessage),
                new UserMessage(userInput)
        );

        ChatOptions chatOptions = ChatOptions.builder()
                .model("gpt-4o-mini")
                .build();

        Prompt prompt = Prompt.builder()
                .messages(messages)
                .chatOptions(chatOptions)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

        return chatModel.call(prompt);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeVectorStore() {
        log.info("Initializing vector store with place data...");
        
        try {
            List<Place> allPlaces = placeRepository.findAll();
            log.info("Found {} places to load into vector store", allPlaces.size());
            
            for (Place place : allPlaces) {
                String document = createDocumentFromPlace(place);
                Map<String, Object> metadata = createMetadataFromPlace(place);
                
                // RagService를 통해 문서 저장 (구체적인 메서드는 RagService 구조에 따라 조정 필요)
                ragService.addDocument(String.valueOf(place.getPlaceId()), document, metadata);
            }
            
            log.info("Successfully loaded {} places into vector store", allPlaces.size());
        } catch (Exception e) {
            log.error("Failed to initialize vector store", e);
        }
    }
    
    private String createDocumentFromPlace(Place place) {
        StringBuilder document = new StringBuilder();
        document.append("장소명: ").append(place.getNameKo()).append("\n");
        document.append("설명: ").append(place.getDescriptionKo()).append("\n");
        document.append("주소: ").append(place.getAddress()).append("\n");
        document.append("지역: ").append(place.getRegion().getNameKo()).append("\n");
        document.append("테마: ").append(String.join(", ", place.getThemes())).append("\n");
        document.append("비용정보: ").append(place.getCostInfo()).append("\n");
        
        if (place.getContactInfo() != null) {
            document.append("연락처: ").append(place.getContactInfo()).append("\n");
        }
        
        return document.toString();
    }
    
    private Map<String, Object> createMetadataFromPlace(Place place) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("placeId", place.getPlaceId());
        metadata.put("nameKo", place.getNameKo());
        metadata.put("nameEn", place.getNameEn());
        metadata.put("regionId", place.getRegion().getRegionId());
        metadata.put("regionName", place.getRegion().getNameKo());
        metadata.put("themes", place.getThemes());
        metadata.put("latitude", place.getLatitude());
        metadata.put("longitude", place.getLongitude());
        return metadata;
    }
    
    
    private int getAvailablePlacesCount(ChatContext context) {
        if (context.getTheme() == null || context.getRegion() == null) {
            return 0;
        }
        
        String themeJson = "\"" + context.getTheme().name().toLowerCase().replace("_", "-") + "\"";
        return placeRepository.countByThemeAndRegion(themeJson, context.getRegion());
    }
    
    private record DaysAdjustmentResult(ChatContext adjustedContext, String adjustmentMessage) {}
    
    private DaysAdjustmentResult adjustDaysIfNeeded(ChatContext context) {
        int availablePlaces = getAvailablePlacesCount(context);
        int requestedPlaces = context.getDays() * 4;
        
        if (requestedPlaces <= availablePlaces) {
            return new DaysAdjustmentResult(context, "");
        }
        
        int maxDays = Math.max(1, availablePlaces / 4);
        ChatContext adjustedContext = ChatContext.builder()
                .theme(context.getTheme())
                .region(context.getRegion())
                .budget(context.getBudget())
                .preferences(context.getPreferences())
                .durationMinutes(context.getDurationMinutes())
                .days(maxDays)
                .build();
        
        String adjustmentMessage = String.format("요청하신 %d일 여행에는 %d개의 장소가 필요하지만, %s 지역의 %s 테마 장소는 총 %d개만 있어서 %d일 여행으로 조정했습니다.\n\n", 
                context.getDays(), requestedPlaces, context.getRegion(), 
                context.getTheme().name(), availablePlaces, maxDays);
        
        return new DaysAdjustmentResult(adjustedContext, adjustmentMessage);
    }
    
    private List<Long> searchAndExtractPlaceIds(ChatContext context, String originalQuery) {
        String searchQuery = buildSearchQuery(context, originalQuery);
        int placesNeeded = context.getDays() * 4;
        
        List<Long> placeIds = ragService.searchPlaceIds(searchQuery, placesNeeded);
        return placeIds.size() >= placesNeeded ? placeIds : null;
    }
    
    private ChatResponse createRouteAndResponse(DaysAdjustmentResult adjustmentResult, List<Long> placeIds) {
        try {
            CreateRouteByPlaceIdsRequestDto routeRequest = CreateRouteByPlaceIdsRequestDto.builder()
                    .placeIds(placeIds)
                    .build();
            
            RouteCreateResponseDto routeResponse = routeService.createRouteByAI(routeRequest);
            
            // RAG를 통해 생성된 루트에 대한 자연스러운 추천 메시지 생성
            String searchQuery = buildSearchQuery(adjustmentResult.adjustedContext(), "루트 추천");
            List<DocumentSearchResult> relevantDocs = ragService.retrieve(searchQuery, 3);
            String aiGeneratedMessage = ragService.generateRouteRecommendationAnswer(
                    adjustmentResult.adjustedContext().getDays() + "일 " + 
                    adjustmentResult.adjustedContext().getTheme().name() + " 테마 루트", 
                    relevantDocs
            );
            
            String finalMessage = adjustmentResult.adjustmentMessage() + aiGeneratedMessage;
            
            return ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.ROUTE_RECOMMENDATION)
                    .message(finalMessage)
                    .routeRecommendation(ChatResponse.RouteRecommendation.builder()
                            .routeId(routeResponse.getRouteId())
                            .endpoint("/api/routes/" + routeResponse.getRouteId())
                            .title(routeResponse.getTitleKo())
                            .description(routeResponse.getDescriptionKo())
                            .estimatedCost((int) routeResponse.getTotalCost())
                            .durationMinutes(routeResponse.getTotalDurationMinutes())
                            .build())
                    .context(adjustmentResult.adjustedContext())
                    .build();
                    
        } catch (Exception e) {
            log.error("루트 생성 중 오류 발생", e);
            return createErrorResponse("루트 생성 중 오류가 발생했습니다. 다시 시도해주시겠어요?", adjustmentResult.adjustedContext());
        }
    }
    
    private ChatResponse createErrorResponse(String message, ChatContext context) {
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message)
                .context(context)
                .build();
    }
    
    
    /**
     * LLM을 사용하여 사용자 질문의 의도를 분류합니다.
     * 기존 키워드 기반 방식보다 정확하고 유연한 분류가 가능합니다.
     */
    private IntentClassificationResult classifyUserIntentWithLLM(String query) {
        String systemPrompt = """
            당신은 한류 여행 챗봇의 의도 분류 전문가입니다.
            사용자의 질문을 분석하여 다음 4가지 의도 중 하나로 정확히 분류해주세요.
            
            **의도 분류 기준:**
            1. CREATE_ROUTE: 새로운 루트/여행 계획 생성을 원하는 경우
               - 예: "2일 서울 K-POP 루트 추천해줘", "부산 여행 계획해줘", "드라마 촬영지 일정 짜줘"
               
            2. SEARCH_EXISTING_ROUTES: 이미 만들어진 기존 루트를 찾고 싶어하는 경우  
               - 예: "기존에 만들어진 부산 드라마 루트 있어?", "이미 있는 K-POP 루트 보여줘"
               
            3. SEARCH_PLACES: 특정 장소나 명소 정보를 찾고 싶어하는 경우
               - 예: "홍대 근처 K-POP 장소 어디 있어?", "명동 뷰티샵 위치 알려줘"
               
            4. GENERAL_QUESTION: 한류나 여행에 관한 일반적인 지식/정보를 묻는 경우
               - 예: "BTS가 뭐야?", "K-드라마 역사 알려줘", "한류가 무엇인가요?"
            
            **중요한 판단 기준:**
            - "추천"이라는 단어가 있어도 "기존 루트"와 함께 사용되면 SEARCH_EXISTING_ROUTES
            - 구체적인 장소명이 언급되고 "어디", "위치" 등이 함께 오면 SEARCH_PLACES  
            - "뭐야", "무엇", "설명", "역사" 등 지식을 묻는 표현이면 GENERAL_QUESTION
            - 위 경우가 아니고 여행 관련이면 대부분 CREATE_ROUTE
            
            **응답 형식 (반드시 JSON으로만 응답):**
            {
                "intent": "CREATE_ROUTE|SEARCH_EXISTING_ROUTES|SEARCH_PLACES|GENERAL_QUESTION",
                "confidence": 0.0~1.0,
                "reasoning": "분류 근거를 한국어로 간단히 설명"
            }
            """;
        
        try {
            org.springframework.ai.chat.model.ChatResponse aiResponse = callOpenAi(query, systemPrompt);
            String responseText = aiResponse.getResult().getOutput().getText();
            
            log.debug("LLM 의도 분류 원본 응답: {}", responseText);
            
            // JSON 파싱
            IntentClassificationResult result = objectMapper.readValue(responseText, IntentClassificationResult.class);
            
            // 신뢰도 검증 (너무 낮으면 fallback)
            if (result.getConfidence() < 0.6) {
                log.warn("LLM 의도 분류 신뢰도가 낮음 ({})... fallback 사용", result.getConfidence());
                return fallbackIntentClassification(query);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("LLM 의도 분류 중 오류 발생: {}", e.getMessage(), e);
            return fallbackIntentClassification(query);
        }
    }
    
    /**
     * LLM 분류가 실패했을 때 사용하는 fallback 분류 로직
     */
    private IntentClassificationResult fallbackIntentClassification(String query) {
        String lowerQuery = query.toLowerCase();
        
        IntentClassificationResult.UserIntent intent;
        String reasoning;
        
        // 기존 루트 검색 키워드 (우선순위 높음)
        if (lowerQuery.contains("기존") || lowerQuery.contains("만들어진") ||
            lowerQuery.contains("있는 루트") || lowerQuery.contains("루트 찾")) {
            intent = IntentClassificationResult.UserIntent.SEARCH_EXISTING_ROUTES;
            reasoning = "기존/만들어진 루트 관련 키워드 감지 (fallback)";
        }
        // 장소 검색 키워드  
        else if (lowerQuery.contains("장소") || lowerQuery.contains("명소") ||
                lowerQuery.contains("어디") || lowerQuery.contains("위치")) {
            intent = IntentClassificationResult.UserIntent.SEARCH_PLACES;
            reasoning = "장소/위치 관련 키워드 감지 (fallback)";
        }
        // 일반 질문 키워드
        else if (lowerQuery.contains("뭐야") || lowerQuery.contains("무엇") ||
                lowerQuery.contains("설명") || lowerQuery.contains("역사")) {
            intent = IntentClassificationResult.UserIntent.GENERAL_QUESTION;
            reasoning = "지식/정보 관련 키워드 감지 (fallback)";
        }
        // 기본값: 루트 생성
        else {
            intent = IntentClassificationResult.UserIntent.CREATE_ROUTE;
            reasoning = "기본값으로 루트 생성 의도로 분류 (fallback)";
        }
        
        return IntentClassificationResult.builder()
                .intent(intent)
                .confidence(0.7) // fallback이므로 중간 신뢰도
                .reasoning(reasoning)
                .build();
    }
    
    /**
     * 새 루트 생성 의도를 처리합니다.
     */
    private ChatResponse handleCreateRouteIntent(ChatRequest request) {
        // 1. 사용자 질문에서 정보 추출
        ChatContext extractedContext = extractContextFromQuery(request.getQuery(), request.getContext());
        
        // 2. 필수 정보 확인
        String missingInfo = checkMissingRequiredInfo(extractedContext);
        if (missingInfo != null) {
            return createQuestionResponse(missingInfo, extractedContext);
        }
        
        // 3. RAG를 통한 루트 생성
        return recommendRouteWithRag(extractedContext, request.getQuery());
    }
    
    /**
     * 기존 루트 검색 의도를 처리합니다.
     * RouteRepository와 RAG를 사용하여 실제 루트를 검색하고 자연스러운 추천 메시지를 생성합니다.
     */
    private ChatResponse handleSearchExistingRoutesIntent(ChatRequest request) {
        // 1. 컨텍스트에서 테마와 지역 정보 추출
        ChatContext extractedContext = extractContextFromQuery(request.getQuery(), request.getContext());
        
        // 2. RouteRepository를 통해 실제 루트 검색
        List<com.mey.backend.domain.route.entity.Route> routes = searchExistingRoutes(extractedContext, request.getQuery());
        
        if (routes.isEmpty()) {
            return createQuestionResponse("요청하신 조건에 맞는 기존 루트를 찾을 수 없습니다. 새로운 루트를 만들어드릴까요?", extractedContext);
        }
        
        // 3. RAG를 통한 자연스러운 추천 메시지 생성
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(request.getQuery(), 3);
        String recommendationMessage = ragService.generateRouteRecommendationAnswer(request.getQuery(), relevantDocs);
        
        // 4. Route 엔티티를 ExistingRoute DTO로 변환
        List<ChatResponse.ExistingRoute> existingRoutes = routes.stream()
                .limit(5) // 최대 5개 루트만 반환
                .map(this::convertRouteToExistingRoute)
                .toList();
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.EXISTING_ROUTES)
                .message(recommendationMessage)
                .existingRoutes(existingRoutes)
                .context(extractedContext)
                .build();
    }
    
    /**
     * 장소 검색 의도를 처리합니다.
     */
    private ChatResponse handleSearchPlacesIntent(ChatRequest request) {
        // 컨텍스트 추출 (장소 검색에도 컨텍스트 활용 가능)
        ChatContext extractedContext = extractContextFromQuery(request.getQuery(), request.getContext());
        
        List<Long> placeIds = ragService.searchPlaceIds(request.getQuery(), 5);
        
        if (placeIds.isEmpty()) {
            return createQuestionResponse("요청하신 조건에 맞는 장소를 찾을 수 없습니다. 다른 키워드로 검색해보시겠어요?", extractedContext);
        }
        
        List<Place> places = placeRepository.findAllById(placeIds);
        List<ChatResponse.PlaceInfo> placeInfos = places.stream()
                .map(place -> ChatResponse.PlaceInfo.builder()
                        .placeId(place.getPlaceId())
                        .name(place.getNameKo())
                        .description(place.getDescriptionKo())
                        .address(place.getAddress())
                        .themes(place.getThemes())
                        .costInfo(place.getCostInfo())
                        .build())
                .toList();
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.PLACE_INFO)
                .message("검색하신 조건에 맞는 장소들을 찾았습니다:")
                .places(placeInfos)
                .context(extractedContext)
                .build();
    }
    
    /**
     * 일반 질문 의도를 처리합니다.
     */
    private ChatResponse handleGeneralQuestionIntent(ChatRequest request) {
        // 일반 질문에서도 컨텍스트 추출 (필요시 활용 가능)
        ChatContext extractedContext = extractContextFromQuery(request.getQuery(), request.getContext());
        
        // RAG를 사용하여 일반적인 질문에 답변
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(request.getQuery(), 3);
        String answer = ragService.generateAnswerWithContexts(request.getQuery(), relevantDocs);
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.GENERAL_INFO)
                .message(answer)
                .context(extractedContext)
                .build();
    }
    
    /**
     * 질문 응답을 생성하는 헬퍼 메서드
     */
    private ChatResponse createQuestionResponse(String message, ChatContext context) {
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message)
                .context(context)
                .build();
    }
    
    /**
     * 컨텍스트와 쿼리를 기반으로 기존 루트를 검색합니다.
     */
    private List<com.mey.backend.domain.route.entity.Route> searchExistingRoutes(ChatContext context, String query) {
        List<com.mey.backend.domain.route.entity.Route> routes = new ArrayList<>();
        
        // 1. 테마와 지역 정보가 있는 경우 우선 검색
        if (context.getTheme() != null && context.getRegion() != null) {
            String themeJson = "[\"" + context.getTheme().getRouteTheme() + "\"]";
            routes = routeRepository.findByThemesAndRegion(themeJson, context.getRegion());
        }
        // 2. 테마만 있는 경우
        else if (context.getTheme() != null) {
            routes = routeRepository.findByThemesContaining(context.getTheme().getRouteTheme());
        }
        // 3. 지역만 있는 경우
        else if (context.getRegion() != null) {
            routes = routeRepository.findByRegionName(context.getRegion());
        }
        // 4. 정보가 없는 경우 인기 루트 반환
        else {
            routes = routeRepository.findAllOrderByPopularity();
        }
        
        return routes;
    }
    
    /**
     * Route 엔티티를 ExistingRoute DTO로 변환합니다.
     */
    private ChatResponse.ExistingRoute convertRouteToExistingRoute(com.mey.backend.domain.route.entity.Route route) {
        // Theme enum을 String으로 변환
        List<String> themeStrings = route.getThemes().stream()
                .map(theme -> theme.getRouteTheme())
                .toList();
        
        return ChatResponse.ExistingRoute.builder()
                .routeId(route.getId())
                .title(route.getTitleKo())
                .description(route.getDescriptionKo())
                .estimatedCost(route.getTotalCost())
                .durationMinutes(route.getTotalDurationMinutes())
                .themes(themeStrings)
                .build();
    }
}
