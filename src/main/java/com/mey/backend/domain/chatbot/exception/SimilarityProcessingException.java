package com.mey.backend.domain.chatbot.exception;

import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.status.ErrorStatus;

public class SimilarityProcessingException extends GeneralException {
    public SimilarityProcessingException() {
        super(ErrorStatus.SIMILARITY_PROCESSING_ERROR);
    }
}
