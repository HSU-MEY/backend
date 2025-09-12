package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.chatbot.dto.ChatResponse;
import com.mey.backend.domain.chatbot.dto.ConversationState;
import com.mey.backend.domain.place.entity.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 챗봇 응답 생성 및 포맷팅을 담당하는 클래스
 * 
 * 주요 책임:
 * - 다양한 타입의 ChatResponse 객체 생성
 * - 질문, 루트 추천, 장소 정보, 에러 등의 응답 빌딩
 * - 테마별 맞춤 메시지 및 이모지 추가
 * - Route 엔티티를 DTO로 변환
 * - 단계별 안내 메시지 생성
 * 
 * 내부 데이터를 사용자 친화적인 응답으로 변환하여
 * 일관된 형태의 챗봇 응답을 제공하는 응답 생성 전문 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatResponseBuilder {
    
    private final ConversationManager conversationManager;
    private final MessageTemplateService messageTemplateService;
    private final LanguageService languageService;
    
    /**
     * 기본 질문 응답을 생성합니다.
     */
    public ChatResponse createQuestionResponse(String message, ChatContext context) {
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message)
                .context(context)
                .build();
    }
    
    /**
     * 상태 기반 질문 응답을 생성합니다.
     */
    public ChatResponse createQuestionResponse(String message, ChatContext context, 
                                               ConversationState newState, String lastBotQuestion) {
        ChatContext updatedContext = context.toBuilder()
                .conversationState(newState)
                .lastBotQuestion(lastBotQuestion)
                .build();
        
        conversationManager.saveSessionContext(context.getSessionId(), updatedContext);
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message)
                .context(updatedContext)
                .build();
    }
    
    /**
     * 에러 응답을 생성합니다.
     */
    public ChatResponse createErrorResponse(String message, ChatContext context) {
        String language = context.getUserLanguage() != null ? context.getUserLanguage() : "ko";
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message + " " + messageTemplateService.getErrorSuffix(language))
                .context(context)
                .build();
    }
    
    /**
     * 일반 정보 응답을 생성합니다.
     */
    public ChatResponse createGeneralInfoResponse(String message, ChatContext context) {
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.GENERAL_INFO)
                .message(message)
                .context(context)
                .build();
    }
    
    /**
     * 장소 정보 응답을 생성합니다.
     */
    public ChatResponse createPlaceInfoResponse(String message, List<Place> places, ChatContext context) {
        String language = context.getUserLanguage() != null ? context.getUserLanguage() : "ko";
        List<ChatResponse.PlaceInfo> placeInfos = places.stream()
                .map(place -> ChatResponse.PlaceInfo.builder()
                        .placeId(place.getPlaceId())
                        .name(languageService.getPlaceName(place, language))
                        .description(languageService.getPlaceDescription(place, language))
                        .address(languageService.getPlaceAddress(place, language))
                        .themes(place.getThemes())
                        .costInfo(place.getCostInfo())
                        .build())
                .toList();
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.PLACE_INFO)
                .message(message)
                .places(placeInfos)
                .context(context)
                .build();
    }
    
    /**
     * 기존 루트 응답을 생성합니다.
     */
    public ChatResponse createExistingRoutesResponse(String message, 
                                                   List<com.mey.backend.domain.route.entity.Route> routes, 
                                                   ChatContext context) {
        String language = context.getUserLanguage() != null ? context.getUserLanguage() : "ko";
        List<ChatResponse.ExistingRoute> existingRoutes = routes.stream()
                .limit(5) // 최대 5개 루트만 반환
                .map(route -> convertRouteToExistingRoute(route, language))
                .toList();
        
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.EXISTING_ROUTES)
                .message(message)
                .existingRoutes(existingRoutes)
                .context(context)
                .build();
    }
    
    /**
     * 루트 추천 응답을 생성합니다.
     */
    public ChatResponse createRouteRecommendationResponse(String message, 
                                                        ChatResponse.RouteRecommendation routeRecommendation,
                                                        ChatContext context) {
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.ROUTE_RECOMMENDATION)
                .message(message)
                .routeRecommendation(routeRecommendation)
                .context(context)
                .build();
    }
    
    /**
     * AI가 생성한 루트 추천 응답을 생성합니다.
     */
    public ChatResponse createAIRouteRecommendationResponse(String message, 
                                                          Long routeId,
                                                          String title,
                                                          String description,
                                                          Integer estimatedCost,
                                                          Integer durationMinutes,
                                                          ChatContext context) {
        ChatResponse.RouteRecommendation routeRecommendation = ChatResponse.RouteRecommendation.builder()
                .routeId(routeId)
                .endpoint("/api/routes/" + routeId)
                .title(title)
                .description(description)
                .estimatedCost(estimatedCost)
                .durationMinutes(durationMinutes)
                .build();
                
        return createRouteRecommendationResponse(message, routeRecommendation, context);
    }
    
    /**
     * Route 엔티티를 ExistingRoute DTO로 변환
     */
    private ChatResponse.ExistingRoute convertRouteToExistingRoute(com.mey.backend.domain.route.entity.Route route, String language) {
        // Theme enum을 String으로 변환
        List<String> themeStrings = route.getThemes().stream()
                .map(theme -> theme.getRouteTheme())
                .toList();
        
        return ChatResponse.ExistingRoute.builder()
                .routeId(route.getId())
                .title(languageService.getRouteTitle(route, language))
                .description(languageService.getRouteDescription(route, language))
                .estimatedCost(route.getTotalCost())
                .durationMinutes(route.getTotalDurationMinutes())
                .themes(themeStrings)
                .build();
    }

    /**
     * 단계별 안내 메시지 생성
     */
    public String generateStepMessage(ConversationState currentState, ChatContext context) {
        String language = context.getUserLanguage() != null ? context.getUserLanguage() : "ko";
        return messageTemplateService.getStateMessage(currentState, language);
    }
}
