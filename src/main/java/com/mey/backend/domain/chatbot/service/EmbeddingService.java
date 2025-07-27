package com.mey.backend.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final OpenAiApi openAiApi;

    private OpenAiEmbeddingModel embeddingModel;

    /**
     * OpenAI 임베딩 모델을 가져옵니다.
     * 처음 호출 시에만 모델을 생성하고, 이후에는 캐시된 인스턴스를 반환합니다.
     *
     * @return OpenAiEmbeddingModel 인스턴스
     */
    public OpenAiEmbeddingModel getEmbeddingModel() {
        if (embeddingModel == null) {
            synchronized (this) {
                if (embeddingModel == null) {
                    embeddingModel = new OpenAiEmbeddingModel(
                            openAiApi,
                            MetadataMode.EMBED,
                            OpenAiEmbeddingOptions.builder()
                                    .model("text-embedding-3-small")
                                    .build(),
                            RetryUtils.DEFAULT_RETRY_TEMPLATE
                    );
                }
            }
        }
        return embeddingModel;
    }
}
