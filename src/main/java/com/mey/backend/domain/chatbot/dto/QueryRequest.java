package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "질의 요청 DTO")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QueryRequest {
    @Schema(description = "사용자 질문", example = "인공지능이란 무엇인가요?")
    @NotBlank
    private String query;

    @Schema(description = "최대 검색 결과 수", example = "3", defaultValue = "3")
    private int maxResults = 3;
}
