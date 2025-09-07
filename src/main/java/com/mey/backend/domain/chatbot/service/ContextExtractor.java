package com.mey.backend.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.chatbot.dto.ChatContext;
import com.mey.backend.domain.route.entity.Theme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사용자 질문에서 컨텍스트 정보 추출을 담당하는 클래스
 * 
 * 주요 책임:
 * - 사용자 질문에서 테마, 지역, 일수, 예산 등의 정보 추출
 * - LLM 기반 정확한 정보 파싱
 * - 정규식 및 키워드 기반 fallback 추출
 * - 기존 컨텍스트와의 스마트 병합
 * - 필수 정보 부족 여부 검증
 * 
 * 이 클래스는 사용자의 자연어 입력을 구조화된 데이터로 변환하여
 * 단계적 정보 수집과 정확한 루트 추천을 가능하게 하는 
 * 정보 처리 엔진입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextExtractor {
    
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 질문에서 전체 컨텍스트를 추출합니다.
     */
    public ChatContext extractContextFromQuery(String query, ChatContext existingContext) {
        String systemPrompt = """
            당신은 사용자 질문에서 한류 루트 추천에 필요한 정보를 추출하는 전문가입니다.
            다음 정보를 JSON 형태로 추출해주세요:
            - theme: 테마 ("KDRAMA", "KPOP", "KFOOD", "KFASHION" 중 하나, 정확히 이 값들 사용)
            - region: 지역명 (서울, 부산 등)
            - budget: 예산 (숫자만, 원 단위)
            - preferences: 특별 선호사항
            - durationMinutes: 소요 시간 (분 단위)
            - days: 여행 일수 (1, 2, 3 등의 숫자)
            
            기존 컨텍스트가 있다면 이를 기반으로 새로운 정보만 업데이트하세요.
            정보가 없거나 추출할 수 없으면 null로 설정하세요.
            
            테마 변환 규칙:
            - "K-POP", "케이팝", "kpop" → "KPOP"
            - "K-드라마", "케이드라마", "kdrama" → "KDRAMA"  
            - "K-푸드", "케이푸드", "kfood" → "KFOOD"
            - "K-패션", "케이패션", "kfashion" → "KFASHION"
            
            응답 형식 (반드시 유효한 JSON):
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
        
        try {
            org.springframework.ai.chat.model.ChatResponse aiResponse = callOpenAi(userMessage, systemPrompt);
            String responseText = removeJsonCodeBlocks(aiResponse.getResult().getOutput().getText().trim());
            
            ChatContext extractedContext = objectMapper.readValue(responseText, ChatContext.class);
            return mergeContexts(existingContext, extractedContext);
        } catch (Exception e) {
            log.error("Failed to parse context from AI response", e);
            return extractContextFallback(query, existingContext);
        }
    }
    
    /**
     * 테마만 추출하는 특화 메서드
     */
    public ChatContext extractThemeFromQuery(String query, ChatContext existingContext) {
        String lowerQuery = query.toLowerCase();
        
        Theme theme = null;
        if (lowerQuery.contains("k-pop") || lowerQuery.contains("kpop") || lowerQuery.contains("케이팝")) {
            theme = Theme.KPOP;
        } else if (lowerQuery.contains("k-drama") || lowerQuery.contains("kdrama") || lowerQuery.contains("드라마") || lowerQuery.contains("케이드라마")) {
            theme = Theme.KDRAMA;
        } else if (lowerQuery.contains("k-food") || lowerQuery.contains("kfood") || lowerQuery.contains("푸드") || lowerQuery.contains("음식") || lowerQuery.contains("케이푸드")) {
            theme = Theme.KFOOD;
        } else if (lowerQuery.contains("k-fashion") || lowerQuery.contains("kfashion") || lowerQuery.contains("패션") || lowerQuery.contains("케이패션")) {
            theme = Theme.KFASHION;
        }
        
        if (theme != null) {
            return existingContext.toBuilder().theme(theme).build();
        }
        
        return existingContext;
    }
    
    /**
     * 지역만 추출하는 특화 메서드
     */
    public ChatContext extractRegionFromQuery(String query, ChatContext existingContext) {
        String extractedRegion = null;
        String lowerQuery = query.toLowerCase();
        
        // 주요 지역 키워드 매칭
        if (lowerQuery.contains("서울")) {
            extractedRegion = "서울";
        } else if (lowerQuery.contains("부산")) {
            extractedRegion = "부산";
        } else if (lowerQuery.contains("제주") || lowerQuery.contains("제주도")) {
            extractedRegion = "제주";
        } else if (lowerQuery.contains("대구")) {
            extractedRegion = "대구";
        } else if (lowerQuery.contains("인천")) {
            extractedRegion = "인천";
        } else if (lowerQuery.contains("경주")) {
            extractedRegion = "경주";
        } else {
            // 더 복잡한 지역명 추출 시도
            extractedRegion = extractRegionWithPattern(query);
        }
        
        if (extractedRegion != null) {
            return existingContext.toBuilder().region(extractedRegion).build();
        }
        
        return existingContext;
    }
    
    /**
     * 일수만 추출하는 특화 메서드
     */
    public ChatContext extractDaysFromQuery(String query, ChatContext existingContext) {
        Integer days = null;
        
        // 숫자 + "일" 패턴 찾기
        Pattern dayPattern = Pattern.compile("(\\d+)일");
        Matcher matcher = dayPattern.matcher(query);
        if (matcher.find()) {
            days = Integer.parseInt(matcher.group(1));
        } else {
            // 단순 숫자 찾기
            Pattern numberPattern = Pattern.compile("\\b([1-9]|1[0-5])\\b");
            Matcher numberMatcher = numberPattern.matcher(query);
            if (numberMatcher.find()) {
                days = Integer.parseInt(numberMatcher.group(1));
            }
        }
        
        if (days != null && days > 0 && days <= 15) {
            return existingContext.toBuilder().days(days).build();
        }
        
        return existingContext;
    }
    
    /**
     * 필수 정보 부족 여부 확인
     */
    public String checkMissingRequiredInfo(ChatContext context) {
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
    
    /**
     * JSON 코드 블록 제거
     */
    private String removeJsonCodeBlocks(String text) {
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            if (text.contains("```")) {
                text = text.substring(0, text.indexOf("```"));
            }
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            if (text.contains("```")) {
                text = text.substring(0, text.indexOf("```"));
            }
        }
        
        // JSON 객체만 추출
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        
        return text.trim();
    }
    
    /**
     * 두 컨텍스트를 병합
     */
    private ChatContext mergeContexts(ChatContext existingContext, ChatContext extractedContext) {
        if (existingContext == null) {
            return extractedContext;
        }
        if (extractedContext == null) {
            return existingContext;
        }
        
        return ChatContext.builder()
                .theme(extractedContext.getTheme() != null ? extractedContext.getTheme() : existingContext.getTheme())
                .region(extractedContext.getRegion() != null ? extractedContext.getRegion() : existingContext.getRegion())
                .budget(extractedContext.getBudget() != null ? extractedContext.getBudget() : existingContext.getBudget())
                .preferences(extractedContext.getPreferences() != null ? extractedContext.getPreferences() : existingContext.getPreferences())
                .durationMinutes(extractedContext.getDurationMinutes() != null ? extractedContext.getDurationMinutes() : existingContext.getDurationMinutes())
                .days(extractedContext.getDays() != null ? extractedContext.getDays() : existingContext.getDays())
                .conversationState(extractedContext.getConversationState() != null ? extractedContext.getConversationState() : existingContext.getConversationState())
                .lastBotQuestion(extractedContext.getLastBotQuestion() != null ? extractedContext.getLastBotQuestion() : existingContext.getLastBotQuestion())
                .sessionId(extractedContext.getSessionId() != null ? extractedContext.getSessionId() : existingContext.getSessionId())
                .conversationStartTime(extractedContext.getConversationStartTime() != null ? extractedContext.getConversationStartTime() : existingContext.getConversationStartTime())
                .build();
    }
    
    /**
     * AI 파싱 실패 시 fallback 로직
     */
    private ChatContext extractContextFallback(String query, ChatContext existingContext) {
        log.info("Using fallback context extraction for query: {}", query);
        
        ChatContext.ChatContextBuilder builder = existingContext != null ? 
                existingContext.toBuilder() : ChatContext.builder();
        
        // 테마 추출
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("k-pop") || lowerQuery.contains("kpop") || lowerQuery.contains("케이팝")) {
            builder.theme(Theme.KPOP);
        } else if (lowerQuery.contains("k-drama") || lowerQuery.contains("kdrama") || lowerQuery.contains("드라마")) {
            builder.theme(Theme.KDRAMA);
        } else if (lowerQuery.contains("k-food") || lowerQuery.contains("kfood") || lowerQuery.contains("푸드") || lowerQuery.contains("음식")) {
            builder.theme(Theme.KFOOD);
        } else if (lowerQuery.contains("k-fashion") || lowerQuery.contains("kfashion") || lowerQuery.contains("패션")) {
            builder.theme(Theme.KFASHION);
        }
        
        // 지역 추출
        if (lowerQuery.contains("서울")) builder.region("서울");
        else if (lowerQuery.contains("부산")) builder.region("부산");
        else if (lowerQuery.contains("제주")) builder.region("제주");
        
        // 일수 추출
        Pattern dayPattern = Pattern.compile("(\\d+)일");
        Matcher matcher = dayPattern.matcher(query);
        if (matcher.find()) {
            builder.days(Integer.parseInt(matcher.group(1)));
        }
        
        // 예산 추출
        Pattern budgetPattern = Pattern.compile("(\\d+)만원|예산.*?(\\d+)");
        Matcher budgetMatcher = budgetPattern.matcher(query);
        if (budgetMatcher.find()) {
            String budgetStr = budgetMatcher.group(1);
            if (budgetStr != null) {
                builder.budget(Integer.parseInt(budgetStr) * 10000);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 패턴을 사용한 지역 추출
     */
    private String extractRegionWithPattern(String query) {
        // 패턴: "지역명으로", "지역명에서", "지역명 여행" 등
        Pattern regionPattern = Pattern.compile("([가-힣]{2,5})(으로|에서|에|로|의|\\s*여행|\\s*투어)");
        Matcher matcher = regionPattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 컨텍스트를 문자열로 변환
     */
    private String convertContextToString(ChatContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    /**
     * OpenAI API 호출
     */
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

        return chatModel.call(prompt);
    }
}