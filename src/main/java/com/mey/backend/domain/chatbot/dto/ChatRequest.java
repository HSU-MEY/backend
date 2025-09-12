package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅 요청 DTO")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @Schema(description = "사용자 질문", example = "k-pop 루트 추천해줘")
    @NotBlank
    private String query;
    
    @Schema(description = "이전 대화에서 추출된 컨텍스트 정보")
    private ChatContext context;
    
    @Schema(description = "사용자 언어", example = "ko", allowableValues = {"ko", "en", "ja", "zh"})
    @Builder.Default
    private String language = "ko";
}
