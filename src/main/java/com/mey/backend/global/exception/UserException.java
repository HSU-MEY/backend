package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class UserException extends GeneralException {
    public UserException(BaseStatus status) {
        super(status);
    }
}
