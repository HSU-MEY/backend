package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class UserRouteException extends GeneralException {
    
    public UserRouteException(BaseStatus status) {
        super(status);
    }
}