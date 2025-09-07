package com.mey.backend.domain.chatbot.dto;

import com.mey.backend.domain.route.entity.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Schema(description = "챗봇 컨텍스트 정보")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatContext {
    
    @Schema(description = "테마 정보", example = "KPOP")
    private Theme theme;
    
    @Schema(description = "지역", example = "서울")
    private String region;
    
    @Schema(description = "예산", example = "50000")
    private Integer budget;
    
    @Schema(description = "선호사항", example = "야외 활동 선호")
    private String preferences;
    
    @Schema(description = "소요 시간 (분)", example = "240")
    private Integer durationMinutes;
    
    @Schema(description = "여행 일수", example = "2")
    private Integer days;
    
    @Schema(description = "현재 대화 상태", example = "AWAITING_THEME")
    private ConversationState conversationState;
    
    @Schema(description = "마지막 봇 질문", example = "어떤 테마의 루트를 찾고 계신가요?")
    private String lastBotQuestion;
    
    @Schema(description = "대화 세션 ID (사용자별 세션 보장)", example = "uuid-1234-5678")
    private String sessionId;
    
    @Schema(description = "대화 시작 시간 (타임스탬프)", example = "1693920000000")
    private Long conversationStartTime;
}