package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class RouteException extends GeneralException {
    
    public RouteException(BaseStatus status) {
        super(status);
    }
}