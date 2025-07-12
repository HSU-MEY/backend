package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class AuthException extends GeneralException {
    public AuthException(BaseStatus status) {
        super(status);
    }
}
