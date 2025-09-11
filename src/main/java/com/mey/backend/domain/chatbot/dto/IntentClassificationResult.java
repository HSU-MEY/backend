package com.mey.backend.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "LLM 기반 의도 분류 결과")
@Getter
@Builder
@AllArgsConstructor
public class IntentClassificationResult {
    
    @Schema(description = "분류된 의도", example = "CREATE_ROUTE")
    private final UserIntent intent;
    
    @Schema(description = "분류 신뢰도 (0.0-1.0)", example = "0.95")
    private final double confidence;
    
    @Schema(description = "분류 근거/이유", example = "사용자가 '추천해줘'라는 표현을 사용했고 여행 계획 의도가 명확함")
    private final String reasoning;
    
    @JsonCreator
    public IntentClassificationResult(
            @JsonProperty("intent") String intent,
            @JsonProperty("confidence") double confidence, 
            @JsonProperty("reasoning") String reasoning) {
        this.intent = UserIntent.fromString(intent);
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
    
    public enum UserIntent {
        CREATE_ROUTE("CREATE_ROUTE", "새로운 루트 생성/추천 요청"),
        SEARCH_EXISTING_ROUTES("SEARCH_EXISTING_ROUTES", "기존에 만들어진 루트 검색 요청"), 
        SEARCH_PLACES("SEARCH_PLACES", "특정 장소나 명소 정보 검색 요청"),
        GENERAL_QUESTION("GENERAL_QUESTION", "한류나 여행에 관한 일반적인 질문");
        
        private final String value;
        private final String description;
        
        UserIntent(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        @JsonCreator
        public static UserIntent fromString(String value) {
            for (UserIntent intent : UserIntent.values()) {
                if (intent.value.equals(value) || intent.name().equals(value)) {
                    return intent;
                }
            }
            throw new IllegalArgumentException("Invalid user intent: " + value);
        }
    }
}