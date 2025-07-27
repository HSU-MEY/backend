package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "문서 업로드 결과 DTO")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadResult {
    @Schema(description = "생성된 문서 ID")
    private String documentId;
}
