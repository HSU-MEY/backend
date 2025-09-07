package com.mey.backend.domain.chatbot.repository;

import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.exception.DocumentProcessingException;
import com.mey.backend.domain.chatbot.exception.SimilarityProcessingException;
import com.mey.backend.domain.chatbot.service.DocumentProcessingService;
import com.mey.backend.domain.chatbot.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 문서를 벡터화하여 저장하고, 벡터 유사도 검색을 제공합니다.
 * Spring AI의 SimpleVectorStore를 활용합니다.
 */
@Repository
@Slf4j
public class InMemoryDocumentVectorStore {

    private final EmbeddingService embeddingService;
    private final DocumentProcessingService documentProcessingService;

    // Spring AI의 인메모리 SimpleVectorStore 생성
    private final VectorStore vectorStore;

    public InMemoryDocumentVectorStore(EmbeddingService embeddingService, DocumentProcessingService documentProcessingService) {
        this.embeddingService = embeddingService;
        this.documentProcessingService = documentProcessingService;
        this.vectorStore = SimpleVectorStore.builder(embeddingService.getEmbeddingModel()).build();
    }

    /**
     * 문서를 벡터 스토어에 추가합니다.
     *
     * @param id 문서 식별자
     * @param fileText 문서 내용
     * @param metadata 문서 메타데이터
     */
    public void addDocument(String id, String fileText, Map<String, Object> metadata) {
        log.debug("문서 추가 시작 - ID: {}, 내용 길이: {}", id, fileText.length());

        try {
            // Spring AI Document 객체 생성
            Map<String, Object> documentMetadata = new HashMap<>(metadata);
            documentMetadata.put("id", id);
            Document document = new Document(fileText, documentMetadata);

            TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                    .withChunkSize(512)           // 원하는 청크 크기
                    .withMinChunkSizeChars(350)   // 최소 청크 크기
                    .withMinChunkLengthToEmbed(5) // 임베딩할 최소 청크 길이
                    .withMaxNumChunks(10000)      // 최대 청크 수
                    .withKeepSeparator(true)      // 구분자 유지 여부
                    .build();

            List<Document> chunks = textSplitter.split(document);

            // 벡터 스토어에 문서 청크 추가 (내부적으로 임베딩 변환 수행)
            vectorStore.add(chunks);

            log.info("문서 추가 완료 - ID: {}", id);
        } catch (Exception e) {
            log.error("문서 추가 실패 - ID: {}", id, e);
            throw new DocumentProcessingException();
        }
    }

    /**
     * 파일을 처리하여 벡터 스토어에 추가합니다.
     *
     * @param id 문서 식별자
     * @param file 파일 객체
     * @param metadata 문서 메타데이터
     */
    public void addDocumentFile(String id, File file, Map<String, Object> metadata) {
        log.debug("파일 문서 추가 시작 - ID: {}, 파일: {}", id, file.getName());

        try {
            // 텍스트 추출
            String fileText;
            String extension = getFileExtension(file.getName()).toLowerCase();

            if ("json".equals(extension)) {
                fileText = documentProcessingService.extractTextFromJson(file);
            } else {
                fileText = Files.readString(file.toPath());
            }

            log.debug("파일 텍스트 추출 완료 - 길이: {}", fileText.length());
            addDocument(id, fileText, metadata);
        } catch (IOException e) {
            log.error("파일 읽기 실패 - ID: {}, 파일: {}", id, file.getName(), e);
            throw new DocumentProcessingException();
        } catch (Exception e) {
            log.error("파일 처리 실패 - ID: {}, 파일: {}", id, file.getName(), e);
            throw new DocumentProcessingException();
        }
    }

    /**
     * 질의와 유사한 문서를 검색합니다.
     *
     * @param query 검색 질의
     * @param maxResults 최대 결과 수
     * @return 유사도 순으로 정렬된 검색 결과 목록
     */
    public List<DocumentSearchResult> similaritySearch(String query, int maxResults) {
        log.debug("유사도 검색 시작 - 질의: '{}', 최대 결과: {}", query, maxResults);

        try {
            // 검색 요청 구성
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(maxResults)
                    .build();

            // 유사성 검색 실행
            List<Document> results = vectorStore.similaritySearch(request);
            if (results == null) {
                results = List.of();
            }

            log.debug("유사도 검색 완료 - 결과 수: {}", results.size());

            // 결과 매핑
            return results.stream()
                    .map(this::mapToSearchResult)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("유사도 검색 실패 - 질의: '{}'", query, e);
            throw new SimilarityProcessingException();
        }
    }

    /**
     * Document를 DocumentSearchResultDto로 매핑합니다.
     */
    private DocumentSearchResult mapToSearchResult(Document result) {
        String id = result.getMetadata().getOrDefault("id", "unknown").toString();
        String content = result.getText() != null ? result.getText() : "";

        // id를 제외한 메타데이터 필터링
        Map<String, Object> filteredMetadata = result.getMetadata().entrySet().stream()
                .filter(entry -> !"id".equals(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        double score = result.getScore() != null ? result.getScore() : 0.0;

        return new DocumentSearchResult(id, content, filteredMetadata, score);
    }

    /**
     * 파일명에서 확장자를 추출합니다.
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
}
