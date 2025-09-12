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
        return classifyUserIntent(query, "ko"); // 기본 한국어
    }
    
    /**
     * 언어를 고려하여 사용자 질문의 의도를 분류합니다.
     */
    public IntentClassificationResult classifyUserIntent(String query, String language) {
        try {
            return classifyWithLLM(query, language);
        } catch (Exception e) {
            log.error("LLM 의도 분류 실패, fallback 사용: {}", e.getMessage());
            return fallbackIntentClassification(query, language);
        }
    }
    
    /**
     * LLM을 사용한 의도 분류
     */
    private IntentClassificationResult classifyWithLLM(String query, String language) {
        String systemPrompt = getSystemPromptByLanguage(language);
        
        org.springframework.ai.chat.model.ChatResponse aiResponse = callOpenAi(query, systemPrompt);
        String responseText = aiResponse.getResult().getOutput().getText().trim();
        
        log.debug("LLM 의도 분류 원본 응답: {}", responseText);
        
        // JSON 파싱
        try {
            IntentClassificationResult result = objectMapper.readValue(responseText, IntentClassificationResult.class);
            
            // 신뢰도 검증 (너무 낮으면 fallback)
            if (result.getConfidence() < 0.6) {
                log.warn("LLM 의도 분류 신뢰도가 낮음 ({})... fallback 사용", result.getConfidence());
                return fallbackIntentClassification(query, language);
            }
            
            log.info("LLM 의도 분류 결과: {} (신뢰도: {})", result.getIntent(), result.getConfidence());
            return result;
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패, fallback 사용: {}", e.getMessage());
            return fallbackIntentClassification(query, language);
        }
    }
    
    /**
     * 키워드 기반 fallback 의도 분류
     */
    private IntentClassificationResult fallbackIntentClassification(String query, String language) {
        String lowerQuery = query.toLowerCase();
        
        // 언어별 키워드로 의도 분류
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> fallbackClassificationKorean(lowerQuery);
            case "en" -> fallbackClassificationEnglish(lowerQuery);
            case "ja" -> fallbackClassificationJapanese(lowerQuery);
            case "zh" -> fallbackClassificationChinese(lowerQuery);
            default -> fallbackClassificationKorean(lowerQuery); // 기본값
        };
    }
    
    private IntentClassificationResult fallbackClassificationKorean(String lowerQuery) {
        // CREATE_ROUTE 키워드
        if (containsAnyKeyword(lowerQuery, "추천", "계획", "루트 만들", "여행 계획", "일정", "만들어줘")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.CREATE_ROUTE, 0.8, "키워드 기반 분류: 루트 생성 요청");
        }
        
        // SEARCH_EXISTING_ROUTES 키워드
        if (containsAnyKeyword(lowerQuery, "기존", "만들어진", "루트 찾", "루트 검색", "있는 루트", "만든 루트")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_EXISTING_ROUTES, 0.8, "키워드 기반 분류: 기존 루트 검색");
        }
        
        // SEARCH_PLACES 키워드 (근처 키워드가 있으면 우선 처리)
        if (containsAnyKeyword(lowerQuery, "근처")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.9, "키워드 기반 분류: 근처 장소 검색");
        }
        
        // SEARCH_PLACES 기타 키워드
        if (containsAnyKeyword(lowerQuery, "장소", "명소", "어디", "위치", "곳", "찾아")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.8, "키워드 기반 분류: 장소 검색");
        }
        
        // 기본값: GENERAL_QUESTION
        return new IntentClassificationResult(IntentClassificationResult.UserIntent.GENERAL_QUESTION, 0.7, "키워드 기반 분류: 일반 질문");
    }
    
    private IntentClassificationResult fallbackClassificationEnglish(String lowerQuery) {
        // CREATE_ROUTE keywords
        if (containsAnyKeyword(lowerQuery, "recommend", "suggest", "plan", "create route", "make itinerary", "trip planning")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.CREATE_ROUTE, 0.8, "Keyword-based classification: Route creation request");
        }
        
        // SEARCH_EXISTING_ROUTES keywords
        if (containsAnyKeyword(lowerQuery, "existing", "available routes", "find route", "search route", "show routes")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_EXISTING_ROUTES, 0.8, "Keyword-based classification: Existing route search");
        }
        
        // SEARCH_PLACES keywords
        if (containsAnyKeyword(lowerQuery, "near", "nearby", "around")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.9, "Keyword-based classification: Nearby place search");
        }
        
        if (containsAnyKeyword(lowerQuery, "place", "location", "where", "find", "spot", "attraction")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.8, "Keyword-based classification: Place search");
        }
        
        // Default: GENERAL_QUESTION
        return new IntentClassificationResult(IntentClassificationResult.UserIntent.GENERAL_QUESTION, 0.7, "Keyword-based classification: General question");
    }
    
    private IntentClassificationResult fallbackClassificationJapanese(String lowerQuery) {
        // CREATE_ROUTE keywords
        if (containsAnyKeyword(lowerQuery, "おすすめ", "計画", "ルート作", "旅行計画", "スケジュール", "作って")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.CREATE_ROUTE, 0.8, "キーワードベース分類: ルート作成要求");
        }
        
        // SEARCH_EXISTING_ROUTES keywords
        if (containsAnyKeyword(lowerQuery, "既存", "作られた", "ルート探", "ルート検索", "あるルート")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_EXISTING_ROUTES, 0.8, "キーワードベース分類: 既存ルート検索");
        }
        
        // SEARCH_PLACES keywords
        if (containsAnyKeyword(lowerQuery, "近く", "付近")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.9, "キーワードベース分類: 近くの場所検索");
        }
        
        if (containsAnyKeyword(lowerQuery, "場所", "名所", "どこ", "位置", "探して", "スポット")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.8, "キーワードベース分類: 場所検索");
        }
        
        // Default: GENERAL_QUESTION
        return new IntentClassificationResult(IntentClassificationResult.UserIntent.GENERAL_QUESTION, 0.7, "キーワードベース分類: 一般的な質問");
    }
    
    private IntentClassificationResult fallbackClassificationChinese(String lowerQuery) {
        // CREATE_ROUTE keywords
        if (containsAnyKeyword(lowerQuery, "推荐", "计划", "路线制", "旅行计划", "行程", "制作")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.CREATE_ROUTE, 0.8, "基于关键词的分类: 路线创建请求");
        }
        
        // SEARCH_EXISTING_ROUTES keywords
        if (containsAnyKeyword(lowerQuery, "现有", "已制作", "路线查找", "路线搜索", "现有路线")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_EXISTING_ROUTES, 0.8, "基于关键词的分类: 现有路线搜索");
        }
        
        // SEARCH_PLACES keywords
        if (containsAnyKeyword(lowerQuery, "附近", "周围")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.9, "基于关键词的分类: 附近地点搜索");
        }
        
        if (containsAnyKeyword(lowerQuery, "地点", "景点", "哪里", "位置", "找", "地方")) {
            return new IntentClassificationResult(IntentClassificationResult.UserIntent.SEARCH_PLACES, 0.8, "基于关键词的分类: 地点搜索");
        }
        
        // Default: GENERAL_QUESTION
        return new IntentClassificationResult(IntentClassificationResult.UserIntent.GENERAL_QUESTION, 0.7, "基于关键词的分类: 一般问题");
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
    
    /**
     * 언어별 시스템 프롬프트를 반환합니다.
     */
    private String getSystemPromptByLanguage(String language) {
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> getKoreanSystemPrompt();
            case "en" -> getEnglishSystemPrompt();
            case "ja" -> getJapaneseSystemPrompt();
            case "zh" -> getChineseSystemPrompt();
            default -> getKoreanSystemPrompt(); // 기본값
        };
    }
    
    private String getKoreanSystemPrompt() {
        return """
                당신은 한류 여행 챗봇의 의도 분류 전문가입니다.
                사용자의 질문을 다음 4가지 의도 중 하나로 분류해주세요:
                
                1. CREATE_ROUTE: 새로운 여행 루트를 만들어달라는 요청
                   - 키워드: "추천해줘", "계획해줘", "루트 만들", "여행 계획", "일정 짜줘"
                   - 예시: "2일 서울 K-POP 루트 추천해줘", "부산 여행 계획해줘"
                
                2. SEARCH_EXISTING_ROUTES: 이미 만들어진 루트를 찾아달라는 요청
                   - 키워드: "기존 루트", "만들어진 루트", "루트 찾아", "루트 검색", "있는 루트"
                   - 예시: "기존에 만들어진 부산 드라마 루트 있어?", "만들어진 K-POP 루트 보여줘"
                
                3. SEARCH_PLACES: 특정 장소나 명소에 대한 정보를 찾는 요청
                   - 키워드: "장소", "명소", "어디", "위치", "곳", "찾아줘", "근처"
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
    }
    
    private String getEnglishSystemPrompt() {
        return """
                You are an intent classification expert for a Korean Wave (Hallyu) travel chatbot.
                Please classify the user's question into one of the following 4 intents:
                
                1. CREATE_ROUTE: Request to create a new travel route
                   - Keywords: "recommend", "suggest", "plan", "create route", "make itinerary"
                   - Examples: "Recommend a 2-day Seoul K-POP route", "Plan a Busan trip"
                
                2. SEARCH_EXISTING_ROUTES: Request to find existing routes
                   - Keywords: "existing routes", "available routes", "find route", "search route"
                   - Examples: "Are there existing Busan drama routes?", "Show me K-POP routes"
                
                3. SEARCH_PLACES: Request for information about specific places or attractions
                   - Keywords: "place", "location", "where", "spot", "find", "near", "around"
                   - Examples: "Where are K-POP places near Hongdae?", "Find restaurants in Myeongdong"
                
                4. GENERAL_QUESTION: General questions about Korean Wave or travel
                   - Keywords: explanation requests, information questions
                   - Examples: "What is BTS?", "What is K-POP?", "Tell me about Hallyu history"
                
                Please respond in JSON format:
                {
                    "intent": "CREATE_ROUTE",
                    "confidence": 0.95,
                    "reasoning": "User is requesting a new travel route"
                }
                """;
    }
    
    private String getJapaneseSystemPrompt() {
        return """
                あなたは韓流旅行チャットボットの意図分類専門家です。
                ユーザーの質問を以下の4つの意図のいずれかに分類してください：
                
                1. CREATE_ROUTE: 新しい旅行ルートを作ってほしいという要求
                   - キーワード: "おすすめ", "計画", "ルート作成", "旅行計画", "スケジュール"
                   - 例: "2日間のソウルK-POPルートをおすすめして", "釜山旅行を計画して"
                
                2. SEARCH_EXISTING_ROUTES: 既存のルートを探してほしいという要求
                   - キーワード: "既存ルート", "作られたルート", "ルート検索", "あるルート"
                   - 例: "釜山ドラマルートはありますか？", "K-POPルートを見せて"
                
                3. SEARCH_PLACES: 特定の場所や観光地についての情報を探す要求
                   - キーワード: "場所", "観光地", "どこ", "位置", "探して", "近く", "付近"
                   - 例: "弘大近くのK-POP場所はどこ？", "明洞のレストランを探して"
                
                4. GENERAL_QUESTION: 韓流や旅行に関する一般的な質問
                   - キーワード: 説明要求, 情報質問
                   - 例: "BTSって何？", "K-POPとは？", "韓流の歴史を教えて"
                
                JSON形式で応答してください：
                {
                    "intent": "CREATE_ROUTE",
                    "confidence": 0.95,
                    "reasoning": "ユーザーが新しい旅行ルートを要求している"
                }
                """;
    }
    
    private String getChineseSystemPrompt() {
        return """
                您是韩流旅行聊天机器人的意图分类专家。
                请将用户的问题分类为以下4个意图之一：
                
                1. CREATE_ROUTE: 请求创建新的旅行路线
                   - 关键词: "推荐", "建议", "计划", "创建路线", "制作行程"
                   - 示例: "推荐2天首尔K-POP路线", "计划釜山旅行"
                
                2. SEARCH_EXISTING_ROUTES: 请求查找现有路线
                   - 关键词: "现有路线", "已有路线", "查找路线", "搜索路线"
                   - 示例: "有釜山韩剧路线吗？", "显示K-POP路线"
                
                3. SEARCH_PLACES: 请求特定地点或景点信息
                   - 关键词: "地点", "景点", "哪里", "位置", "查找", "附近", "周围"
                   - 示例: "弘大附近的K-POP地点在哪？", "找明洞餐厅"
                
                4. GENERAL_QUESTION: 关于韩流或旅行的一般问题
                   - 关键词: 解释请求, 信息询问
                   - 示例: "BTS是什么？", "什么是K-POP？", "告诉我韩流历史"
                
                请以JSON格式回答：
                {
                    "intent": "CREATE_ROUTE",
                    "confidence": 0.95,
                    "reasoning": "用户请求创建新的旅行路线"
                }
                """;
    }
}
