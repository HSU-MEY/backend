package com.mey.backend.domain.chatbot.exception;

import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.status.BaseStatus;

public class DocumentProcessingException extends GeneralException {
    protected DocumentProcessingException(BaseStatus status) {
        super(status);
    }
}
