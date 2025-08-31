package com.mey.backend.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.dto.ChatResponse;
import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import java.util.Arrays;
import java.util.List;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResponse processUserQuery(ChatRequest request) {
        log.info("Processing user query: {}", request.getQuery());
        
        // 1. 사용자 질문에서 정보 추출
        ChatContext extractedContext = extractContextFromQuery(request.getQuery(), request.getContext());
        
        // 2. 필수 정보 확인
        String missingInfo = checkMissingRequiredInfo(extractedContext);
        if (missingInfo != null) {
            return ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.QUESTION)
                    .message(missingInfo)
                    .build();
        }
        
        // 3. RAG를 통한 루트 추천
        return recommendRouteWithRag(extractedContext, request.getQuery());
    }

    private ChatContext extractContextFromQuery(String query, ChatContext existingContext) {
        String systemPrompt = """
            당신은 사용자 질문에서 한류 루트 추천에 필요한 정보를 추출하는 전문가입니다.
            다음 정보를 JSON 형태로 추출해주세요:
            - themes: 테마 목록 (KDRAMA, KPOP, KFOOD, KFASHION 중)
            - region: 지역명 (서울, 부산 등)
            - budget: 예산 (숫자만, 원 단위)
            - preferences: 특별 선호사항
            - durationMinutes: 소요 시간 (분 단위)
            
            기존 컨텍스트가 있다면 이를 기반으로 새로운 정보만 업데이트하세요.
            정보가 없으면 null로 설정하세요.
            
            응답 형식:
            {
                "themes": ["KPOP"],
                "region": "서울",
                "budget": 50000,
                "preferences": null,
                "durationMinutes": null
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
        if (context.getThemes() == null || context.getThemes().isEmpty()) {
            return "어떤 테마의 루트를 찾고 계신가요? (K-POP, K-드라마, K-푸드, K-패션 중 선택해주세요)";
        }
        
        if (context.getRegion() == null || context.getRegion().trim().isEmpty()) {
            return "어느 지역의 루트를 원하시나요? (예: 서울, 부산)";
        }
        
        return null; // 필수 정보가 모두 있음
    }

    private ChatResponse recommendRouteWithRag(ChatContext context, String originalQuery) {
        // RAG를 통한 루트 정보 검색
        String searchQuery = buildSearchQuery(context, originalQuery);
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(searchQuery, 5);
        
        if (relevantDocs.isEmpty()) {
            return ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.QUESTION)
                    .message("죄송합니다. 요청하신 조건에 맞는 루트 정보를 찾을 수 없습니다. 다른 테마나 지역을 시도해보시겠어요?")
                    .build();
        }
        
        // RAG 기반 추천 메시지 생성
        String responseMessage = ragService.generateRouteRecommendationAnswer(originalQuery, relevantDocs);
        
        // 첫 번째 문서에서 루트 정보 추출 (간단한 파싱)
        DocumentSearchResult firstDoc = relevantDocs.get(0);
        RouteInfo routeInfo = extractRouteInfoFromDocument(firstDoc);
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.ROUTE_RECOMMENDATION)
                .message(responseMessage)
                .routeRecommendation(ChatResponse.RouteRecommendation.builder()
                        .routeId(routeInfo.routeId)
                        .endpoint("/api/routes/" + routeInfo.routeId)
                        .title(routeInfo.title)
                        .description(routeInfo.description)
                        .estimatedCost(routeInfo.estimatedCost)
                        .durationMinutes(routeInfo.durationMinutes)
                        .build())
                .build();
    }
    
    private String buildSearchQuery(ChatContext context, String originalQuery) {
        StringBuilder searchQuery = new StringBuilder(originalQuery);
        
        if (context.getThemes() != null && !context.getThemes().isEmpty()) {
            searchQuery.append(" ").append(context.getThemes().get(0).name());
        }
        
        if (context.getRegion() != null) {
            searchQuery.append(" ").append(context.getRegion());
        }
        
        if (context.getBudget() != null) {
            searchQuery.append(" 예산 ").append(context.getBudget()).append("원");
        }
        
        return searchQuery.toString();
    }
    
    private RouteInfo extractRouteInfoFromDocument(DocumentSearchResult doc) {
        // 문서에서 루트 정보 추출 (실제 구현에서는 더 정교한 파싱 필요)
        String content = doc.getContent();
        
        // 기본값 설정
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.routeId = 1L; // 실제로는 문서에서 추출해야 함
        routeInfo.title = "한류 추천 루트";
        routeInfo.description = "RAG 기반으로 선별된 한류 루트입니다.";
        routeInfo.estimatedCost = 50000;
        routeInfo.durationMinutes = 240;
        
        // 문서 내용에서 정보 추출 시도
        try {
            // 간단한 파싱 로직 (실제로는 더 정교하게 구현 필요)
            if (content.contains("루트ID:")) {
                String[] parts = content.split("루트ID:");
                if (parts.length > 1) {
                    String idStr = parts[1].split("\n")[0].trim();
                    routeInfo.routeId = Long.parseLong(idStr);
                }
            }
            
            if (content.contains("제목:")) {
                String[] parts = content.split("제목:");
                if (parts.length > 1) {
                    routeInfo.title = parts[1].split("\n")[0].trim();
                }
            }
            
            if (content.contains("설명:")) {
                String[] parts = content.split("설명:");
                if (parts.length > 1) {
                    routeInfo.description = parts[1].split("\n")[0].trim();
                }
            }
        } catch (Exception e) {
            log.warn("문서에서 루트 정보 추출 중 오류 발생: {}", e.getMessage());
        }
        
        return routeInfo;
    }
    
    private static class RouteInfo {
        Long routeId;
        String title;
        String description;
        Integer estimatedCost;
        Integer durationMinutes;
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
}
