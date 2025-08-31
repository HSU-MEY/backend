package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "문서 검색 결과 DTO")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DocumentSearchResult {
    @Schema(description = "문서 ID")
    private String id;

    @Schema(description = "문서 내용")
    private String content;

    @Schema(description = "문서 메타데이터")
    private Map<String, Object> metadata;

    @Schema(description = "유사도 점수")
    private double similarityScore;
}
