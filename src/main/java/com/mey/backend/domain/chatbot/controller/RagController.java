package com.mey.backend.domain.chatbot.controller;

import com.mey.backend.domain.chatbot.dto.DocumentResponse;
import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.dto.DocumentUploadResult;
import com.mey.backend.domain.chatbot.dto.QueryRequest;
import com.mey.backend.domain.chatbot.dto.QueryResponse;
import com.mey.backend.domain.chatbot.exception.FileProcessingException;
import com.mey.backend.domain.chatbot.service.RagService;
import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.payload.status.ErrorStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG API", description = "Retrieval-Augmented Generation 기능을 위한 API")
public class RagController {

    private final RagService ragService;

    /**
     * JSON 문서를 업로드하여 벡터 스토어에 저장합니다.
     */
    @Operation(
            summary = "json 문서 업로드",
            description = "json 파일을 업로드하여 벡터 스토어에 저장합니다. 추후 질의에 활용됩니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 업로드 성공"
    )
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 파일 또는 json이 아닌 파일)")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<DocumentUploadResult> uploadDocument(
            @Parameter(description = "업로드할 JSON 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        log.info("문서 업로드 요청 받음: {}", file.getOriginalFilename());

        // 유효성 검사
        if (file.isEmpty()) {
            log.warn("빈 파일이 업로드됨");
            throw new GeneralException(ErrorStatus.EMPTY_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            log.warn("지원하지 않는 파일 형식: {}", originalFilename);
            throw new GeneralException(ErrorStatus.INVALID_FILE_TYPE);
        }

        // File 객체 생성
        File tempFile;
        try {
            tempFile = File.createTempFile("upload_", ".json");
            log.debug("임시 파일 생성됨: {}", tempFile.getAbsolutePath());
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error("임시 파일 생성 실패", e);
            throw new FileProcessingException();
        }

        // 문서 처리 및 응답
        try {
            String documentId = ragService.uploadJsonFile(tempFile, originalFilename);

            log.info("문서 업로드 성공: {}", documentId);
            return CommonResponse.onSuccess(new DocumentUploadResult(documentId));
        } catch (Exception e) {
            log.error("문서 처리 중 오류 발생", e);
            throw new FileProcessingException();
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (deleted) {
                    log.debug("임시 파일 삭제됨: {}", tempFile.getAbsolutePath());
                } else {
                    log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 사용자 질의에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성합니다.
     */
    @Operation(
            summary = "RAG 질의 수행",
            description = "사용자 질문에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "질의 성공"
    )
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping("/query")
    public CommonResponse<QueryResponse> queryWithRag(
            @Parameter(description = "질의 요청 객체", required = true)
            @RequestBody QueryRequest request
    ) {
        log.info("RAG 질의 요청 받음: {}", request.getQuery());

        // 관련 문서 검색
        List<DocumentSearchResult> relevantDocs = ragService.retrieve(
                request.getQuery(),
                request.getMaxResults()
        );

        // RAG 기반 응답 생성
        String answer = ragService.generateAnswerWithContexts(
                request.getQuery(),
                relevantDocs
        );

        // DocumentSearchResultDto를 DocumentResponseDto로 변환
        List<DocumentResponse> documentResponses = relevantDocs.stream()
                .map(DocumentSearchResult::toDocumentResponse)
                .collect(Collectors.toList());

        return CommonResponse.onSuccess(new QueryResponse(request.getQuery(), answer, documentResponses));
    }
}
