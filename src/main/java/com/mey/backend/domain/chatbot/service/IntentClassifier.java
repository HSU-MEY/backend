package com.mey.backend.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.chatbot.dto.IntentClassificationResult;
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

/**
 * 사용자 의도 분류를 담당하는 클래스
 * 
 * 주요 책임:
 * - 사용자 질문의 의도 분류 (CREATE_ROUTE, SEARCH_EXISTING_ROUTES, SEARCH_PLACES, GENERAL_QUESTION)
 * - LLM 기반 고정밀 의도 분류
 * - 키워드 기반 fallback 분류
 * - 분류 신뢰도 검증 및 품질 보장
 * 
 * 챗봇이 사용자의 질문을 정확히 이해하고 적절한 로직으로 라우팅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {
    
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    
    /**
     * 사용자 질문의 의도를 분류합니다.
     */
    public IntentClassificationResult classifyUserIntent(String query) {
        try {
            return classifyWithLLM(query);
        } catch (Exception e) {
            log.error("LLM 의도 분류 실패, fallback 사용: {}", e.getMessage());
            return fallbackIntentClassification(query);
        }
    }
    
    /**
     * LLM을 사용한 의도 분류
     */
    private IntentClassificationResult classifyWithLLM(String query) {
        String systemPrompt = """
                당신은 한류 여행 챗봇의 의도 분류 전문가입니다.
                사용자의 질문을 다음 4가지 의도 중 하나로 분류해주세요:
                
                1. CREATE_ROUTE: 새로운 여행 루트를 만들어달라는 요청
                   - 키워드: "추천해줘", "계획해줘", "루트 만들", "여행 계획", "일정 짜줘"
                   - 예시: "2일 서울 K-POP 루트 추천해줘", "부산 여행 계획해줘"
                
                2. SEARCH_EXISTING_ROUTES: 이미 만들어진 루트를 찾아달라는 요청
                   - 키워드: "기존 루트", "만들어진 루트", "루트 찾아", "루트 검색", "있는 루트"
                   - 예시: "기존에 만들어진 부산 드라마 루트 있어?", "만들어진 K-POP 루트 보여줘"
                
                3. SEARCH_PLACES: 특정 장소나 명소에 대한 정보를 찾는 요청
                   - 키워드: "장소", "명소", "어디", "위치", "곳", "찾아줘"
                   - 예시: "홍대 근처 K-POP 장소 어디 있어?", "명동 맛집 찾아줘"
                
                4. GENERAL_QUESTION: 한류나 여행에 대한 일반적인 질문
                   - 키워드: 설명 요청, 정보 질문
                   - 예시: "BTS가 뭐야?", "K-POP이란?", "한류 역사 알려줘"
                
                JSON 형식으로 응답해주세요:
                {
                    "intent": "CREATE_ROUTE",
                    "confidence": 0.95,
                    "reasoning": "사용자가 새로운 여행 루트를 요청함"
                }
                """;
        
        org.springframework.ai.chat.model.ChatResponse aiResponse = callOpenAi(query, systemPrompt);
        String responseText = aiResponse.getResult().getOutput().getText().trim();
        
        log.debug("LLM 의도 분류 원본 응답: {}", responseText);
        
        // JSON 파싱
        try {
            IntentClassificationResult result = objectMapper.readValue(responseText, IntentClassificationResult.class);
            
            // 신뢰도 검증 (너무 낮으면 fallback)
            if (result.getConfidence() < 0.6) {
                log.warn("LLM 의도 분류 신뢰도가 낮음 ({})... fallback 사용", result.getConfidence());
                return fallbackIntentClassification(query);
            }
            
            log.info("LLM 의도 분류 결과: {} (신뢰도: {})", result.getIntent(), result.getConfidence());
            return result;
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패, fallback 사용: {}", e.getMessage());
            return fallbackIntentClassification(query);
        }
    }
    
    /**
     * 키워드 기반 fallback 의도 분류
     */
    private IntentClassificationResult fallbackIntentClassification(String query) {
        String lowerQuery = query.toLowerCase();
        
        // CREATE_ROUTE 키워드
        if (containsAnyKeyword(lowerQuery, "추천", "계획", "루트 만들", "여행 계획", "일정", "만들어줘")) {
            return new IntentClassificationResult("CREATE_ROUTE", 0.8, "키워드 기반 분류: 루트 생성 요청");
        }
        
        // SEARCH_EXISTING_ROUTES 키워드
        if (containsAnyKeyword(lowerQuery, "기존", "만들어진", "루트 찾", "루트 검색", "있는 루트", "만든 루트")) {
            return new IntentClassificationResult("SEARCH_EXISTING_ROUTES", 0.8, "키워드 기반 분류: 기존 루트 검색");
        }
        
        // SEARCH_PLACES 키워드
        if (containsAnyKeyword(lowerQuery, "장소", "명소", "어디", "위치", "곳", "찾아", "근처")) {
            return new IntentClassificationResult("SEARCH_PLACES", 0.8, "키워드 기반 분류: 장소 검색");
        }
        
        // 기본값: GENERAL_QUESTION
        return new IntentClassificationResult("GENERAL_QUESTION", 0.7, "키워드 기반 분류: 일반 질문");
    }
    
    /**
     * 키워드 포함 여부 확인
     */
    private boolean containsAnyKeyword(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
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
