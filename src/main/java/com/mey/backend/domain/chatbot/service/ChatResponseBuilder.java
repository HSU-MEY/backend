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
        return ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message(message + " 다시 시도해주시겠어요?")
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
        List<ChatResponse.ExistingRoute> existingRoutes = routes.stream()
                .limit(5) // 최대 5개 루트만 반환
                .map(this::convertRouteToExistingRoute)
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
     * Route 엔티티를 ExistingRoute DTO로 변환
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
    
    /**
     * 테마별 맞춤 메시지 생성
     */
    public String generateThemeBasedMessage(ChatContext context, String baseMessage) {
        if (context.getTheme() == null) {
            return baseMessage;
        }
        
        String themeEmoji = switch (context.getTheme()) {
            case KPOP -> "🎵";
            case KDRAMA -> "📺";
            case KFOOD -> "🍜";
            case KFASHION -> "👗";
        };
        
        return themeEmoji + " " + baseMessage;
    }
    
    /**
     * 단계별 안내 메시지 생성
     */
    public String generateStepMessage(ConversationState currentState, ChatContext context) {
        return switch (currentState) {
            case AWAITING_THEME -> "1️⃣ 테마 선택이 필요해요. K-POP, K-드라마, K-푸드, K-패션 중 어떤 테마를 원하시나요?";
            case AWAITING_REGION -> "2️⃣ 좋습니다! " + (context.getTheme() != null ? context.getTheme().name() : "") + " 테마를 선택하셨네요. 어느 지역을 여행하고 싶으신가요?";
            case AWAITING_DAYS -> "3️⃣ " + (context.getRegion() != null ? context.getRegion() : "") + " 지역을 선택하셨네요! 몇 일 여행을 계획하고 계신가요?";
            case READY_FOR_ROUTE -> "✅ 모든 정보가 준비되었습니다! 맞춤 루트를 생성해드릴게요.";
            default -> "안내에 따라 정보를 입력해주세요.";
        };
    }
}
