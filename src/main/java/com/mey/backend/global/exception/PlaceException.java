package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;

public class PlaceException extends GeneralException {

    public PlaceException(BaseStatus status) {
        super(status);
    }
}
