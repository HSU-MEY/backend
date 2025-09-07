package com.mey.backend.domain.chatbot.dto;

import com.mey.backend.domain.route.entity.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Schema(description = "챗봇 컨텍스트 정보")
@Getter
@Builder
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
}