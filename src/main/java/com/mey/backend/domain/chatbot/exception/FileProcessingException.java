package com.mey.backend.domain.chatbot.exception;

import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.status.ErrorStatus;

public class FileProcessingException extends GeneralException {
    public FileProcessingException() {
        super(ErrorStatus.FILE_PROCESSING_ERROR);
    }
}
