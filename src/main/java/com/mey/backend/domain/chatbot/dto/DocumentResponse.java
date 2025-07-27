package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "문서 응답 DTO")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DocumentResponse {
    @Schema(description = "문서 ID")
    private String documentId;

    @Schema(description = "유사도 점수")
    private Double similarityScore;

    @Schema(description = "문서 내용 (일부)")
    private String contentSnippet;

    @Schema(description = "문서 메타데이터")
    Map<String, Object> metadata;
}
