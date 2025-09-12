package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.ConversationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 다국어 메시지 템플릿 관리 서비스
 * 
 * 주요 책임:
 * - 언어별 시스템 메시지 및 사용자 응답 템플릿 관리
 * - 대화 상태별 안내 메시지 제공
 * - 에러 메시지 및 확인 메시지 다국어 지원
 */
@Slf4j
@Service
public class MessageTemplateService {
    
    // 필수 정보 부족 시 안내 메시지
    private static final Map<String, Map<String, String>> MISSING_INFO_MESSAGES = Map.of(
        "theme", Map.of(
            "ko", "어떤 테마의 루트를 찾고 계신가요? (K-POP, K-드라마, K-푸드, K-패션 중 선택해주세요)",
            "en", "What theme are you looking for? (Please choose from K-POP, K-Drama, K-Food, K-Fashion)",
            "ja", "どのようなテーマをお探しですか？（K-POP、K-ドラマ、K-フード、K-ファッションからお選びください）",
            "zh", "您在寻找什么主题？（请从K-POP、K-Drama、K-Food、K-Fashion中选择）"
        ),
        "region", Map.of(
            "ko", "어느 지역의 루트를 원하시나요? (예: 서울, 부산)",
            "en", "Which region would you like to visit? (e.g., Seoul, Busan)",
            "ja", "どちらの地域をご希望ですか？（例：ソウル、釜山）",
            "zh", "您希望去哪个地区？（例如：首尔、釜山）"
        ),
        "days", Map.of(
            "ko", "몇 일 여행을 계획하고 계신가요? (예: 1일, 2일, 3일)",
            "en", "How many days are you planning to travel? (e.g., 1 day, 2 days, 3 days)",
            "ja", "何日間の旅行を計画していますか？（例：1日、2日、3日）",
            "zh", "您计划旅行几天？（例如：1天、2天、3天）"
        )
    );
    
    // 대화 상태별 안내 메시지
    private static final Map<ConversationState, Map<String, String>> STATE_MESSAGES = Map.of(
        ConversationState.AWAITING_THEME, Map.of(
            "ko", "테마 선택이 필요해요. K-POP, K-드라마, K-푸드, K-패션 중 어떤 테마를 원하시나요?",
            "en", "Theme selection is needed. Which theme would you like: K-POP, K-Drama, K-Food, or K-Fashion?",
            "ja", "テーマの選択が必要です。K-POP、K-ドラマ、K-フード、K-ファッションのどのテーマをご希望ですか？",
            "zh", "需要选择主题。您希望选择哪个主题：K-POP、K-Drama、K-Food还是K-Fashion？"
        ),
        ConversationState.AWAITING_REGION, Map.of(
            "ko", "어느 지역을 여행하고 싶으신가요?",
            "en", "Which region would you like to travel to?",
            "ja", "どちらの地域を旅行したいですか？",
            "zh", "您想去哪个地区旅行？"
        ),
        ConversationState.AWAITING_DAYS, Map.of(
            "ko", "몇 일 여행을 계획하고 계신가요?",
            "en", "How many days are you planning to travel?",
            "ja", "何日間の旅行を計画していますか？",
            "zh", "您计划旅行几天？"
        ),
        ConversationState.READY_FOR_ROUTE, Map.of(
            "ko", "모든 정보가 준비되었습니다! 맞춤 루트를 생성해드릴게요.",
            "en", "All information is ready! I'll create a customized route for you.",
            "ja", "すべての情報が準備できました！カスタマイズされたルートを作成いたします。",
            "zh", "所有信息都已准备就绪！我将为您创建定制路线。"
        )
    );
    
    // 에러 메시지
    private static final Map<String, String> ERROR_MESSAGES = Map.of(
        "ko", "다시 시도해주시겠어요?",
        "en", "Would you like to try again?",
        "ja", "もう一度お試しいただけますか？",
        "zh", "您愿意再试一次吗？"
    );
    
    // 검색 결과 없음 메시지
    private static final Map<String, String> NO_RESULTS_MESSAGES = Map.of(
        "ko", "요청하신 조건에 맞는 정보를 찾을 수 없습니다. 다른 조건으로 검색해보시겠어요?",
        "en", "No information found matching your criteria. Would you like to try different conditions?",
        "ja", "お客様の条件に合う情報が見つかりませんでした。他の条件で検索してみませんか？",
        "zh", "未找到符合您条件的信息。您想尝试其他条件吗？"
    );
    
    // 테마 확인 메시지
    private static final Map<String, String> THEME_CONFIRMATION_TEMPLATES = Map.of(
        "ko", "좋습니다! {theme} 테마를 선택하셨네요. 어느 지역의 루트를 원하시나요? (예: 서울, 부산)",
        "en", "Great! You've selected the {theme} theme. Which region would you like to visit? (e.g., Seoul, Busan)",
        "ja", "素晴らしい！{theme}テーマを選択されましたね。どちらの地域をご希望ですか？（例：ソウル、釜山）",
        "zh", "太好了！您选择了{theme}主题。您希望去哪个地区？（例如：首尔、釜山）"
    );
    
    // 지역 확인 메시지
    private static final Map<String, String> REGION_CONFIRMATION_TEMPLATES = Map.of(
        "ko", "{region} 지역을 선택하셨네요! 몇 일 여행을 계획하고 계신가요? (예: 1일, 2일, 3일)",
        "en", "You've selected {region}! How many days are you planning to travel? (e.g., 1 day, 2 days, 3 days)",
        "ja", "{region}を選択されましたね！何日間の旅行を計画していますか？（例：1日、2日、3日）",
        "zh", "您选择了{region}！您计划旅行几天？（例如：1天、2天、3天）"
    );
    
    // 인식 실패 메시지
    private static final Map<String, Map<String, String>> RECOGNITION_FAILURE_MESSAGES = Map.of(
        "theme", Map.of(
            "ko", "테마를 인식하지 못했습니다. K-POP, K-드라마, K-푸드, K-패션 중에서 선택해주세요.",
            "en", "I couldn't recognize the theme. Please choose from K-POP, K-Drama, K-Food, K-Fashion.",
            "ja", "テーマを認識できませんでした。K-POP、K-ドラマ、K-フード、K-ファッションからお選びください。",
            "zh", "无法识别主题。请从K-POP、K-Drama、K-Food、K-Fashion中选择。"
        ),
        "region", Map.of(
            "ko", "지역을 인식하지 못했습니다. 구체적인 지역명을 말씀해주세요. (예: 서울, 부산, 제주도)",
            "en", "I couldn't recognize the region. Please provide a specific region name. (e.g., Seoul, Busan, Jeju)",
            "ja", "地域を認識できませんでした。具体的な地域名を教えてください。（例：ソウル、釜山、済州島）",
            "zh", "无法识别地区。请提供具体的地区名称。（例如：首尔、釜山、济州岛）"
        ),
        "days", Map.of(
            "ko", "일수를 인식하지 못했습니다. 숫자로 말씀해주세요. (예: 1일, 2일, 3일)",
            "en", "I couldn't recognize the number of days. Please provide a number. (e.g., 1 day, 2 days, 3 days)",
            "ja", "日数を認識できませんでした。数字で教えてください。（例：1日、2日、3日）",
            "zh", "无法识别天数。请用数字告诉我。（例如：1天、2天、3天）"
        )
    );
    
    // 일반 정보 응답 시작 문구
    private static final Map<String, String> PLACE_INFO_HEADERS = Map.of(
        "ko", "검색하신 조건에 맞는 장소들을 찾았습니다:",
        "en", "I found places matching your search criteria:",
        "ja", "お客様の検索条件に合う場所を見つけました：",
        "zh", "我找到了符合您搜索条件的地点："
    );
    
    /**
     * 필수 정보 부족 메시지를 반환합니다.
     */
    public String getMissingInfoMessage(String infoType, String language) {
        Map<String, String> messages = MISSING_INFO_MESSAGES.get(infoType);
        if (messages == null) {
            return MISSING_INFO_MESSAGES.get("theme").get("ko"); // 기본값
        }
        
        return messages.getOrDefault(language, messages.get("ko"));
    }
    
    /**
     * 대화 상태별 안내 메시지를 반환합니다.
     */
    public String getStateMessage(ConversationState state, String language) {
        Map<String, String> messages = STATE_MESSAGES.get(state);
        if (messages == null) {
            return "";
        }
        
        return messages.getOrDefault(language, messages.get("ko"));
    }
    
    /**
     * 에러 메시지를 반환합니다.
     */
    public String getErrorSuffix(String language) {
        return ERROR_MESSAGES.getOrDefault(language, ERROR_MESSAGES.get("ko"));
    }
    
    /**
     * 검색 결과 없음 메시지를 반환합니다.
     */
    public String getNoResultsMessage(String language) {
        return NO_RESULTS_MESSAGES.getOrDefault(language, NO_RESULTS_MESSAGES.get("ko"));
    }
    
    /**
     * 테마 확인 메시지를 반환합니다.
     */
    public String getThemeConfirmationMessage(String theme, String language) {
        String template = THEME_CONFIRMATION_TEMPLATES.getOrDefault(language, 
            THEME_CONFIRMATION_TEMPLATES.get("ko"));
        return template.replace("{theme}", theme);
    }
    
    /**
     * 지역 확인 메시지를 반환합니다.
     */
    public String getRegionConfirmationMessage(String region, String language) {
        String template = REGION_CONFIRMATION_TEMPLATES.getOrDefault(language, 
            REGION_CONFIRMATION_TEMPLATES.get("ko"));
        return template.replace("{region}", region);
    }
    
    /**
     * 인식 실패 메시지를 반환합니다.
     */
    public String getRecognitionFailureMessage(String infoType, String language) {
        Map<String, String> messages = RECOGNITION_FAILURE_MESSAGES.get(infoType);
        if (messages == null) {
            return RECOGNITION_FAILURE_MESSAGES.get("theme").get("ko"); // 기본값
        }
        
        return messages.getOrDefault(language, messages.get("ko"));
    }
    
    /**
     * 장소 정보 헤더 메시지를 반환합니다.
     */
    public String getPlaceInfoHeader(String language) {
        return PLACE_INFO_HEADERS.getOrDefault(language, PLACE_INFO_HEADERS.get("ko"));
    }
}