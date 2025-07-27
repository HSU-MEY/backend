package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "질의 응답 DTO")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QueryResponse {
    @Schema(description = "원본 질의")
    private String query;

    @Schema(description = "생성된 답변")
    private String answer;

    @Schema(description = "관련 문서 목록")
    private List<DocumentResponse> relatedDocuments;
}
