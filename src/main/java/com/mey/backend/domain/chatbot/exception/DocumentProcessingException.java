package com.mey.backend.domain.chatbot.exception;

import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.status.BaseStatus;
import com.mey.backend.global.payload.status.ErrorStatus;

public class DocumentProcessingException extends GeneralException {
    public DocumentProcessingException() {
        super(ErrorStatus.DOCUMENT_EMBEDDING_ERROR);
    }
}
