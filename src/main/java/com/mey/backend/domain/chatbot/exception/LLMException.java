package com.mey.backend.domain.chatbot.exception;

import com.mey.backend.global.exception.GeneralException;
import com.mey.backend.global.payload.status.ErrorStatus;

public class LLMException extends GeneralException {
    public LLMException() {
        super(ErrorStatus.LLM_API_ERROR);
    }
}
