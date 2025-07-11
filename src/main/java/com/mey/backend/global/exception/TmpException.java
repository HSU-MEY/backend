package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class TmpException extends GeneralException {
    public TmpException(BaseStatus status) {
        super(status);
    }
}
