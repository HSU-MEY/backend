package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "채팅 요청 DTO")
@Getter
@AllArgsConstructor
public class ChatRequest {
    @Schema(description = "사용자 질문", example = "안녕하세요")
    @NotBlank
    private String query;
}
