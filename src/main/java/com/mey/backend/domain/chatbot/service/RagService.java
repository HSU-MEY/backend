package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.exception.DocumentProcessingException;
import com.mey.backend.domain.chatbot.repository.InMemoryDocumentVectorStore;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final InMemoryDocumentVectorStore vectorStore;
    private final OpenAiApi openAiApi;

    /**
     * json 파일을 업로드하여 벡터 스토어에 추가합니다.
     *
     * @param file PDF 파일
     * @param originalFilename 원본 파일명
     * @return 생성된 문서 ID
     */
    public String uploadJsonFile(File file, String originalFilename) {
        String documentId = UUID.randomUUID().toString();
        log.info("PDF 문서 업로드 시작. 파일: {}, ID: {}", originalFilename, documentId);

        // 메타데이터 준비
        Map<String, Object> docMetadata = new HashMap<>();
        docMetadata.put("originalFilename", originalFilename != null ? originalFilename : "");
        docMetadata.put("uploadTime", System.currentTimeMillis());

        // 벡터 스토어에 문서 추가
        try {
            vectorStore.addDocumentFile(documentId, file, docMetadata);
            log.info("PDF 문서 업로드 완료. ID: {}", documentId);
            return documentId;
        } catch (Exception e) {
            log.error("문서 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new DocumentProcessingException();
        }
    }

    /**
     * 질의와 관련된 문서를 검색합니다.
     *
     * @param question 사용자 질문
     * @param maxResults 최대 검색 결과 수
     * @return 유사도 순으로 정렬된 문서 목록
     */
    public List<DocumentSearchResult> retrieve(String question, int maxResults) {
        log.debug("검색 시작: '{}', 최대 결과 수: {}", question, maxResults);
        return vectorStore.similaritySearch(question, maxResults);
    }

    /**
     * 한류 루트 추천을 위한 답변을 생성합니다 (출처 정보 없음).
     *
     * @param question 사용자 질문
     * @param relevantDocs 이미 검색된 관련 문서
     * @return 출처 정보가 제거된 루트 추천 응답
     */
    public String generateRouteRecommendationAnswer(String question, List<DocumentSearchResult> relevantDocs) {
        log.debug("루트 추천 응답 생성 시작: '{}'", question);

        // 관련 문서 검색 또는 사용
        if (relevantDocs.isEmpty()) {
            log.info("관련 정보를 찾을 수 없음: '{}'", question);
            return "죄송합니다. 요청하신 조건에 맞는 루트 정보를 찾을 수 없습니다. 다른 테마나 지역을 시도해보시겠어요?";
        }

        // 관련 문서의 내용을 컨텍스트로 결합 (번호 없이)
        String context = relevantDocs.stream()
                .map(DocumentSearchResult::getContent)
                .collect(java.util.stream.Collectors.joining("\n\n"));
        
        log.debug("컨텍스트 크기: {} 문자", context.length());

        // 루트 추천에 특화된 시스템 프롬프트 생성
        String systemPromptText = String.format("""
            당신은 친근하고 전문적인 한류 여행 가이드입니다. 
            사용자의 질문에 대해 주어진 루트 정보를 바탕으로 자연스럽고 매력적인 추천을 해주세요.
            
            다음 원칙을 따라주세요:
            1. 정보 출처나 참고 번호는 절대 언급하지 마세요
            2. 자연스럽고 친근한 톤으로 응답하세요
            3. 루트의 특징과 매력을 강조하세요
            4. 구체적인 추천 이유를 포함하세요
            5. 2-3문장으로 간결하게 작성하세요
            
            루트 정보:
            %s
            """, context);

        // LLM을 통한 응답 생성
        try {
            ChatResponse response = callOpenAi(question, systemPromptText);
            log.debug("AI 응답 생성: {}", response);
            String aiAnswer = (response != null && response.getResult() != null &&
                    response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getText()
                    : "응답을 생성할 수 없습니다.";

            return aiAnswer;
        } catch (Exception e) {
            log.error("AI 모델 호출 중 오류 발생: {}", e.getMessage(), e);
            return "죄송합니다. 현재 추천 시스템에 문제가 있습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * 질문에 대한 답변을 생성하며, 참고한 정보 출처도 함께 제공합니다.
     *
     * @param question 사용자 질문
     * @param relevantDocs 이미 검색된 관련 문서
     * @return 참고 출처가 포함된 응답
     */
    public String generateAnswerWithContexts(String question, List<DocumentSearchResult> relevantDocs) {
        log.debug("RAG 응답 생성 시작: '{}'", question);

        // 관련 문서 검색 또는 사용
        if (relevantDocs.isEmpty()) {
            log.info("관련 정보를 찾을 수 없음: '{}'", question);
            return "관련 정보를 찾을 수 없습니다. 다른 질문을 시도하거나 관련 문서를 업로드해 주세요.";
        }

        // 문서 번호 부여 (응답에서 출처 표시를 위해)
        List<String> numberedDocs = IntStream.range(0, relevantDocs.size())
                .mapToObj(index -> "[" + (index + 1) + "] " + relevantDocs.get(index).getContent())
                .toList();

        // 관련 문서의 내용을 컨텍스트로 결합
        String context = String.join("\n\n", numberedDocs);
        log.debug("컨텍스트 크기: {} 문자", context.length());

        // 컨텍스트를 포함하는 시스템 프롬프트 생성
        String systemPromptText = String.format("""
            당신은 지식 기반 Q&A 시스템입니다. 
            사용자의 질문에 대한 답변을 다음 정보를 바탕으로 생성해주세요.
            주어진 정보에 답이 없다면 모른다고 솔직히 말해주세요.
            답변 마지막에 사용한 정보의 출처 번호 [1], [2] 등을 반드시 포함해주세요.
            
            정보:
            %s
            """, context);

        // LLM을 통한 응답 생성
        try {
            ChatResponse response = callOpenAi(question, systemPromptText);
            log.debug("AI 응답 생성: {}", response);
            String aiAnswer = (response != null && response.getResult() != null &&
                    response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getText()
                    : "응답을 생성할 수 없습니다.";

            // 참고 문서 정보 추가
            StringBuilder sourceInfo = new StringBuilder();
            sourceInfo.append("\n\n참고 문서:");
            for (int i = 0; i < relevantDocs.size(); i++) {
                DocumentSearchResult doc = relevantDocs.get(i);
                String originalFilename = doc.getMetadata().getOrDefault("originalFilename", "Unknown file").toString();
                sourceInfo.append("\n[").append(i + 1).append("] ").append(originalFilename);
            }

            return aiAnswer + sourceInfo.toString();
        } catch (Exception e) {
            log.error("AI 모델 호출 중 오류 발생: {}", e.getMessage(), e);
            StringBuilder fallbackResponse = new StringBuilder();
            fallbackResponse.append("AI 모델 호출 중 오류가 발생했습니다. 검색 결과만 제공합니다:\n\n");

            for (DocumentSearchResult doc : relevantDocs) {
                fallbackResponse.append(doc.getContent()).append("\n\n");
            }

            return fallbackResponse.toString();
        }
    }

    private ChatResponse callOpenAi(String userInput, String systemMessage) {
        List<Message> messages = Arrays.asList(
                new SystemMessage(systemMessage),
                new UserMessage(userInput)
        );

        ChatOptions chatOptions = ChatOptions.builder()
                .model("gpt-4o-mini")
                .build();

        Prompt prompt = Prompt.builder()
                .messages(messages)
                .chatOptions(chatOptions)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

        return chatModel.call(prompt);
    }

}
