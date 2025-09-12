package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.dto.ChatResponse;
import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.dto.IntentClassificationResult;
import com.mey.backend.domain.chatbot.dto.ConversationState;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.route.dto.CreateRouteByPlaceIdsRequestDto;
import com.mey.backend.domain.route.dto.RouteCreateResponseDto;
import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.service.RouteService;
import com.mey.backend.domain.route.repository.RouteRepository;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final RagService ragService;
    private final PlaceRepository placeRepository;
    private final RouteService routeService;
    private final RouteRepository routeRepository;

    private final ConversationManager conversationManager;
    private final IntentClassifier intentClassifier;
    private final ContextExtractor contextExtractor;
    private final ChatResponseBuilder responseBuilder;
    private final LanguageService languageService;
    private final MessageTemplateService messageTemplateService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initializeVectorStore() {
        // 재시도 로직으로 SQL 데이터 로딩 대기
        int maxRetries = 5;
        int retryCount = 0;
        List<Place> allPlaces = null;

        while (retryCount < maxRetries) {
            try {
                allPlaces = placeRepository.findAll();
                if (!allPlaces.isEmpty()) {
                    break; // 데이터가 있으면 중단
                }

                Thread.sleep(1000); // 1초 대기 후 재시도
                retryCount++;

            } catch (Exception e) {
                retryCount++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (allPlaces == null || allPlaces.isEmpty()) {
            log.warn("{}번 시도했음에도 DB에서 장소 정보를 로드할 수 없습니다. Vector Store가 비어있게 됩니다.", maxRetries);
            return;
        }

        try {
            for (Place place : allPlaces) {
                String document = createDocumentFromPlace(place);
                Map<String, Object> metadata = createMetadataFromPlace(place);

                ragService.addDocument(String.valueOf(place.getPlaceId()), document, metadata);
            }
        } catch (Exception e) {
            log.error("Vector Store 초기화를 실패했습니다.", e);
        }
    }

    /**
     * 사용자 쿼리를 처리하여 적절한 응답을 반환합니다.
     * 상태 기반 대화 처리를 지원하며, 세션 연속성을 보장합니다.
     */
    public ChatResponse processUserQuery(ChatRequest request) {
        log.info("Processing user query: {} (language: {})", request.getQuery(), request.getLanguage());

        // 1. 언어 검증 및 설정
        String validatedLanguage = languageService.validateAndGetLanguage(request.getLanguage());
        log.info("Validated language: {}", validatedLanguage);

        // 2. 세션 보장 및 컨텍스트 가져오기 (언어 정보 포함)
        ChatContext context = conversationManager.ensureSessionAndGetContext(request);
        context = context.toBuilder().userLanguage(validatedLanguage).build();
        log.info("Current conversation state: {}, SessionId: {}, Language: {}",
                context.getConversationState(), context.getSessionId(), context.getUserLanguage());

        // 3. 상태 기반 대화 처리
        if (conversationManager.requiresStatefulHandling(context)) {
            return handleStatefulConversation(request, context);
        }

        // 4. 초기 상태 또는 상태 없음 - 의도 분류 수행 (언어 고려)
        IntentClassificationResult classificationResult = intentClassifier.classifyUserIntent(request.getQuery(), validatedLanguage);
        log.info("LLM 의도 분류 결과: {} (신뢰도: {}, 근거: {})",
                classificationResult.getIntent(),
                classificationResult.getConfidence(),
                classificationResult.getReasoning());

        // 5. 의도별 처리
        return switch (classificationResult.getIntent()) {
            case CREATE_ROUTE -> handleCreateRouteIntent(request.toBuilder().context(context).build());
            case SEARCH_EXISTING_ROUTES -> handleSearchExistingRoutesIntent(request.toBuilder().context(context).build());
            case SEARCH_PLACES -> handleSearchPlacesIntent(request.toBuilder().context(context).build());
            case GENERAL_QUESTION -> handleGeneralQuestionIntent(request.toBuilder().context(context).build());
        };
    }

    /**
     * 상태 기반 대화 처리
     */
    private ChatResponse handleStatefulConversation(ChatRequest request, ChatContext context) {
        return switch (context.getConversationState()) {
            case AWAITING_THEME -> handleThemeInput(request, context);
            case AWAITING_REGION -> handleRegionInput(request, context);
            case AWAITING_DAYS -> handleDaysInput(request, context);
            default -> {
                log.warn("알 수 없는 대화 상태: {}, 초기화합니다.", context.getConversationState());
                ChatContext resetContext = conversationManager.resetConversationState(context);
                yield processUserQuery(request.toBuilder().context(resetContext).build());
            }
        };
    }

    /**
     * 테마 입력 처리
     */
    private ChatResponse handleThemeInput(ChatRequest request, ChatContext context) {
        log.info("Processing theme input: {}", request.getQuery());

        // 테마 추출
        ChatContext updatedContext = contextExtractor.extractThemeFromQuery(request.getQuery(), context);

        if (updatedContext.getTheme() == null) {
            String language = context.getUserLanguage();
            return responseBuilder.createQuestionResponse(
                    messageTemplateService.getRecognitionFailureMessage("theme", language),
                    context.toBuilder().build(), // 상태 유지
                    ConversationState.AWAITING_THEME,
                    messageTemplateService.getMissingInfoMessage("theme", language)
            );
        }

        // 다음 단계로 이동 - 지역 질문
        ChatContext nextContext = updatedContext.toBuilder()
                .conversationState(ConversationState.AWAITING_REGION)
                .lastBotQuestion("어느 지역의 루트를 원하시나요? (예: 서울, 부산)")
                .build();

        conversationManager.saveSessionContext(nextContext.getSessionId(), nextContext);

        String language = context.getUserLanguage();
        return responseBuilder.createQuestionResponse(
                messageTemplateService.getThemeConfirmationMessage(updatedContext.getTheme().name(), language),
                nextContext,
                ConversationState.AWAITING_REGION,
                messageTemplateService.getMissingInfoMessage("region", language)
        );
    }

    /**
     * 지역 입력 처리
     */
    private ChatResponse handleRegionInput(ChatRequest request, ChatContext context) {
        log.info("Processing region input: {}", request.getQuery());

        // 지역 추출
        ChatContext updatedContext = contextExtractor.extractRegionFromQuery(request.getQuery(), context);

        if (updatedContext.getRegion() == null || updatedContext.getRegion().trim().isEmpty()) {
            String language = context.getUserLanguage();
            return responseBuilder.createQuestionResponse(
                    messageTemplateService.getRecognitionFailureMessage("region", language),
                    context.toBuilder().build(), // 상태 유지
                    ConversationState.AWAITING_REGION,
                    messageTemplateService.getMissingInfoMessage("region", language)
            );
        }

        // 다음 단계로 이동 - 일수 질문
        ChatContext nextContext = updatedContext.toBuilder()
                .conversationState(ConversationState.AWAITING_DAYS)
                .lastBotQuestion("몇 일 여행을 계획하고 계신가요? (예: 1일, 2일, 3일)")
                .build();

        conversationManager.saveSessionContext(nextContext.getSessionId(), nextContext);

        String language = context.getUserLanguage();
        return responseBuilder.createQuestionResponse(
                messageTemplateService.getRegionConfirmationMessage(updatedContext.getRegion(), language),
                nextContext,
                ConversationState.AWAITING_DAYS,
                messageTemplateService.getMissingInfoMessage("days", language)
        );
    }

    /**
     * 일수 입력 처리
     */
    private ChatResponse handleDaysInput(ChatRequest request, ChatContext context) {
        log.info("Processing days input: {}", request.getQuery());

        // 일수 추출
        ChatContext updatedContext = contextExtractor.extractDaysFromQuery(request.getQuery(), context);

        if (updatedContext.getDays() == null || updatedContext.getDays() <= 0) {
            String language = context.getUserLanguage();
            return responseBuilder.createQuestionResponse(
                    messageTemplateService.getRecognitionFailureMessage("days", language),
                    context.toBuilder().build(), // 상태 유지
                    ConversationState.AWAITING_DAYS,
                    messageTemplateService.getMissingInfoMessage("days", language)
            );
        }

        // 모든 필수 정보가 수집됨 - 루트 생성
        ChatContext completeContext = updatedContext.toBuilder()
                .conversationState(ConversationState.READY_FOR_ROUTE)
                .lastBotQuestion(null)
                .build();

        conversationManager.saveSessionContext(completeContext.getSessionId(), completeContext);

        return recommendRouteWithRag(completeContext,
                completeContext.getDays() + "일 " +
                completeContext.getRegion() + " " +
                completeContext.getTheme().name() + " 루트");
    }

    private ChatResponse recommendRouteWithRag(ChatContext context, String originalQuery) {
        // 1. 장소 검색 먼저 수행
        String searchQuery = buildSearchQuery(context, originalQuery);
        int placesNeeded = context.getDays() * 4;
        
        List<Long> placeIds = ragService.searchPlaceIds(searchQuery, placesNeeded);
        if (placeIds.isEmpty()) {
            String language = context.getUserLanguage();
            return responseBuilder.createErrorResponse(messageTemplateService.getNoResultsMessage(language), context);
        }

        // 2. 실제 검색된 장소 수를 기반으로 일수 조정
        DaysAdjustmentResult adjustmentResult = adjustDaysBasedOnFoundPlaces(context, placeIds.size());

        // 3. 필요한 만큼 장소 ID 제한
        int actualPlacesNeeded = adjustmentResult.adjustedContext().getDays() * 4;
        if (placeIds.size() > actualPlacesNeeded) {
            placeIds = placeIds.subList(0, actualPlacesNeeded);
        }

        // 4. 루트 생성 및 응답
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

    private String createDocumentFromPlace(Place place) {
        return createDocumentFromPlace(place, "ko"); // 기본 한국어로 벡터 스토어 구성
    }
    
    private String createDocumentFromPlace(Place place, String language) {
        StringBuilder document = new StringBuilder();
        
        // 언어별 필드 활용
        String placeName = languageService.getPlaceName(place, language);
        String placeDescription = languageService.getPlaceDescription(place, language);
        String placeAddress = languageService.getPlaceAddress(place, language);
        
        document.append("장소명: ").append(placeName).append("\n");
        document.append("설명: ").append(placeDescription).append("\n");
        document.append("주소: ").append(placeAddress).append("\n");
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
        
        String themeJson = "\"" + context.getTheme().name() + "\"";
        log.debug("장소 개수 조회 - 테마: {}, 지역: {}, themeJson: {}", context.getTheme(), context.getRegion(), themeJson);
        int count = placeRepository.countByThemeAndRegion(themeJson, context.getRegion());
        log.debug("장소 개수 조회 결과: {}", count);
        return count;
    }

    private record DaysAdjustmentResult(ChatContext adjustedContext, String adjustmentMessage) {}
    
    private DaysAdjustmentResult adjustDaysBasedOnFoundPlaces(ChatContext context, int foundPlacesCount) {
        int requestedPlaces = context.getDays() * 4;
        
        if (requestedPlaces <= foundPlacesCount) {
            return new DaysAdjustmentResult(context, "");
        }
        
        int maxDays = Math.max(1, foundPlacesCount / 4);
        ChatContext adjustedContext = ChatContext.builder()
                .theme(context.getTheme())
                .region(context.getRegion())
                .budget(context.getBudget())
                .preferences(context.getPreferences())
                .durationMinutes(context.getDurationMinutes())
                .days(maxDays)
                .conversationState(context.getConversationState())
                .lastBotQuestion(context.getLastBotQuestion())
                .sessionId(context.getSessionId())
                .conversationStartTime(context.getConversationStartTime())
                .build();
        
        String adjustmentMessage = String.format("요청하신 %d일 여행에는 %d개의 장소가 필요하지만, %s 지역의 %s 테마 장소는 총 %d개만 있어서 %d일 여행으로 조정했습니다.\n\n", 
                context.getDays(), requestedPlaces, context.getRegion(), 
                context.getTheme().name(), foundPlacesCount, maxDays);
        
        return new DaysAdjustmentResult(adjustedContext, adjustmentMessage);
    }
    
    
    private ChatResponse createRouteAndResponse(DaysAdjustmentResult adjustmentResult, List<Long> placeIds) {
        try {
            CreateRouteByPlaceIdsRequestDto routeRequest = CreateRouteByPlaceIdsRequestDto.builder()
                    .placeIds(placeIds)
                    .build();
            
            RouteCreateResponseDto routeResponse = routeService.createRouteByAI(routeRequest);
            
            // 실제 생성된 루트의 장소 정보를 순서대로 정렬하여 AI 메시지 생성
            List<Place> allPlaces = placeRepository.findAllById(placeIds);
            
            // placeIds의 순서대로 Place 객체들을 정렬
            List<Place> routePlaces = placeIds.stream()
                    .map(id -> allPlaces.stream()
                            .filter(place -> place.getPlaceId().equals(id))
                            .findFirst()
                            .orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
                    
            String language = adjustmentResult.adjustedContext().getUserLanguage();
            String aiGeneratedMessage = ragService.generateRouteRecommendationAnswerWithPlaces(
                    adjustmentResult.adjustedContext().getDays() + "일 " + 
                    adjustmentResult.adjustedContext().getTheme().name() + " 테마 루트",
                    routePlaces,
                    language
            );
            
            String finalMessage = adjustmentResult.adjustmentMessage() + aiGeneratedMessage;
            
            // 더 의미있는 제목과 설명 생성
            ChatContext context = adjustmentResult.adjustedContext();
            String customTitle = String.format("%s %s %d일 루트", 
                    context.getRegion(), 
                    context.getTheme().name().replace("_", "-"), 
                    context.getDays());
            
            return responseBuilder.createAIRouteRecommendationResponse(
                    finalMessage,
                    routeResponse.getRouteId(),
                    customTitle,
                    routeResponse.getDescriptionKo(),
                    (int) routeResponse.getTotalCost(),
                    routeResponse.getTotalDurationMinutes(),
                    adjustmentResult.adjustedContext()
            );
                    
        } catch (Exception e) {
            log.error("루트 생성 중 오류 발생", e);
            String language = adjustmentResult.adjustedContext().getUserLanguage();
            return responseBuilder.createErrorResponse(messageTemplateService.getNoResultsMessage(language), adjustmentResult.adjustedContext());
        }
    }
    
    /**
     * 새 루트 생성 의도를 처리합니다.
     */
    private ChatResponse handleCreateRouteIntent(ChatRequest request) {
        // 1. 사용자 질문에서 정보 추출
        ChatContext extractedContext = contextExtractor.extractContextFromQuery(request.getQuery(), request.getContext());
        
        // 2. 필수 정보 확인
        String missingInfo = contextExtractor.checkMissingRequiredInfo(extractedContext);
        if (missingInfo != null) {
            // 상태 기반 대화 시작 - 첫 번째 누락 항목에 따라 상태 설정
            String language = extractedContext.getUserLanguage();
            ConversationState nextState;
            String question;
            
            if (extractedContext.getTheme() == null) {
                nextState = ConversationState.AWAITING_THEME;
                question = messageTemplateService.getMissingInfoMessage("theme", language);
            } else if (extractedContext.getRegion() == null || extractedContext.getRegion().trim().isEmpty()) {
                nextState = ConversationState.AWAITING_REGION;
                question = messageTemplateService.getMissingInfoMessage("region", language);
            } else {
                nextState = ConversationState.AWAITING_DAYS;
                question = messageTemplateService.getMissingInfoMessage("days", language);
            }
            
            return responseBuilder.createQuestionResponse(missingInfo, extractedContext, nextState, question);
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
        ChatContext extractedContext = contextExtractor.extractContextFromQuery(request.getQuery(), request.getContext());
        
        // 2. RouteRepository를 통해 실제 루트 검색
        List<com.mey.backend.domain.route.entity.Route> routes = searchExistingRoutes(extractedContext, request.getQuery());
        
        if (routes.isEmpty()) {
            String language = extractedContext.getUserLanguage();
            return responseBuilder.createQuestionResponse(messageTemplateService.getNoResultsMessage(language), extractedContext);
        }
        
        // 3. RAG를 통한 자연스러운 추천 메시지 생성 (언어 고려)
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(request.getQuery(), 3);
        String language = extractedContext.getUserLanguage();
        String recommendationMessage = ragService.generateRouteRecommendationAnswer(request.getQuery(), relevantDocs, language);
        
        // 4. Route 엔티티를 ExistingRoute DTO로 변환
        return responseBuilder.createExistingRoutesResponse(recommendationMessage, routes, extractedContext);
    }
    
    /**
     * 장소 검색 의도를 처리합니다.
     */
    private ChatResponse handleSearchPlacesIntent(ChatRequest request) {
        // 컨텍스트 추출 (장소 검색에도 컨텍스트 활용 가능)
        ChatContext extractedContext = contextExtractor.extractContextFromQuery(request.getQuery(), request.getContext());
        
        List<Long> placeIds = ragService.searchPlaceIds(request.getQuery(), 5);
        
        if (placeIds.isEmpty()) {
            String language = extractedContext.getUserLanguage();
            return responseBuilder.createQuestionResponse(messageTemplateService.getNoResultsMessage(language), extractedContext);
        }
        
        List<Place> places = placeRepository.findAllById(placeIds);
        String language = extractedContext.getUserLanguage();
        return responseBuilder.createPlaceInfoResponse(messageTemplateService.getPlaceInfoHeader(language), places, extractedContext);
    }
    
    /**
     * 일반 질문 의도를 처리합니다.
     */
    private ChatResponse handleGeneralQuestionIntent(ChatRequest request) {
        // 일반 질문에서도 컨텍스트 추출 (필요시 활용 가능)
        ChatContext extractedContext = contextExtractor.extractContextFromQuery(request.getQuery(), request.getContext());
        
        // RAG를 사용하여 일반적인 질문에 답변
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(request.getQuery(), 3);
        String answer = ragService.generateAnswerWithContexts(request.getQuery(), relevantDocs);
        
        return responseBuilder.createGeneralInfoResponse(answer, extractedContext);
    }

    /**
     * 컨텍스트와 쿼리를 기반으로 기존 루트를 검색합니다.
     */
    private List<Route> searchExistingRoutes(ChatContext context, String query) {
        // 1. 테마와 지역 정보가 있는 경우 우선 검색
        if (context.getTheme() != null && context.getRegion() != null) {
            String themeJson = "[\"" + context.getTheme().getRouteTheme() + "\"]";
            return routeRepository.findByThemesAndRegion(themeJson, context.getRegion());
        }

        // 2. 테마만 있는 경우
        if (context.getTheme() != null) {
            return routeRepository.findByThemesContaining(context.getTheme().getRouteTheme());
        }

        // 3. 지역만 있는 경우
        if (context.getRegion() != null) {
            return routeRepository.findByRegionName(context.getRegion());
        }

        // 4. 정보가 없는 경우 인기 루트 반환
        return routeRepository.findAllOrderByPopularity();
    }
    
}
