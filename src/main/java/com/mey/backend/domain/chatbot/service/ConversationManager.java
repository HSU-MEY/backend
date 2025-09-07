package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.dto.ConversationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대화 세션 및 상태 관리를 담당하는 클래스
 * 
 * 주요 책임:
 * - 사용자별 세션 생성 및 관리
 * - 대화 컨텍스트 저장/조회/병합
 * - 대화 상태 전환 로직
 * - 세션 ID 보장 및 안전성 관리
 * 
 * 상태 기반 챗봇의 핵심 세션 관리자 역할을 수행
 * 여러 사용자가 동시에 대화할 때 각자의 독립적인 컨텍스트를 보장
 */
@Slf4j
@Component
public class ConversationManager {
    
    private final Map<String, ChatContext> sessionContextMap = new ConcurrentHashMap<>();
    
    /**
     * 세션을 보장하고 컨텍스트를 가져옵니다.
     */
    public ChatContext ensureSessionAndGetContext(ChatRequest request) {
        ChatContext context = request.getContext();
        
        if (context == null) {
            String newSessionId = UUID.randomUUID().toString();
            context = ChatContext.builder()
                    .sessionId(newSessionId)
                    .conversationState(ConversationState.INITIAL)
                    .conversationStartTime(System.currentTimeMillis())
                    .build();
            log.info("Created new session: {}", newSessionId);
            return context;
        }
        
        String sessionId = context.getSessionId();
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            context = context.toBuilder()
                    .sessionId(sessionId)
                    .conversationStartTime(System.currentTimeMillis())
                    .build();
            log.warn("Context sessionId was null, generated new one: {}", sessionId);
        }
        
        // 기존 세션에서 더 상세한 정보가 있다면 병합
        ChatContext existingContext = sessionContextMap.get(sessionId);
        if (existingContext != null) {
            context = mergeContexts(existingContext, context);
        }
        
        return context;
    }
    
    /**
     * 세션 컨텍스트를 저장합니다.
     */
    public void saveSessionContext(String sessionId, ChatContext context) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            context = context.toBuilder().sessionId(sessionId).build();
            log.warn("Context sessionId was null, generated new one: {}", sessionId);
        }
        sessionContextMap.put(sessionId, context);
        log.debug("Saved session context for sessionId: {}", sessionId);
    }
    
    /**
     * 세션 컨텍스트를 가져옵니다.
     */
    public ChatContext getSessionContext(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionContextMap.get(sessionId);
    }
    
    /**
     * 세션을 삭제합니다.
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessionContextMap.remove(sessionId);
            log.info("Removed session: {}", sessionId);
        }
    }
    
    /**
     * 대화 상태가 상태 기반 처리가 필요한지 확인합니다.
     */
    public boolean requiresStatefulHandling(ChatContext context) {
        return context != null 
                && context.getConversationState() != null 
                && context.getConversationState() != ConversationState.INITIAL;
    }
    
    /**
     * 대화 상태를 초기화합니다.
     */
    public ChatContext resetConversationState(ChatContext context) {
        ChatContext resetContext = context.toBuilder()
                .conversationState(ConversationState.INITIAL)
                .lastBotQuestion(null)
                .build();
        
        saveSessionContext(context.getSessionId(), resetContext);
        return resetContext;
    }
    
    /**
     * 두 컨텍스트를 병합합니다.
     */
    private ChatContext mergeContexts(ChatContext existingContext, ChatContext newContext) {
        return ChatContext.builder()
                .theme(newContext.getTheme() != null ? newContext.getTheme() : existingContext.getTheme())
                .region(newContext.getRegion() != null ? newContext.getRegion() : existingContext.getRegion())
                .budget(newContext.getBudget() != null ? newContext.getBudget() : existingContext.getBudget())
                .preferences(newContext.getPreferences() != null ? newContext.getPreferences() : existingContext.getPreferences())
                .durationMinutes(newContext.getDurationMinutes() != null ? newContext.getDurationMinutes() : existingContext.getDurationMinutes())
                .days(newContext.getDays() != null ? newContext.getDays() : existingContext.getDays())
                .conversationState(newContext.getConversationState() != null ? newContext.getConversationState() : existingContext.getConversationState())
                .lastBotQuestion(newContext.getLastBotQuestion() != null ? newContext.getLastBotQuestion() : existingContext.getLastBotQuestion())
                .sessionId(newContext.getSessionId() != null ? newContext.getSessionId() : existingContext.getSessionId())
                .conversationStartTime(newContext.getConversationStartTime() != null ? newContext.getConversationStartTime() : existingContext.getConversationStartTime())
                .build();
    }
}
