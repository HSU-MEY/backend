package com.mey.backend.global.exception;

import com.mey.backend.global.payload.status.BaseStatus;
import com.mey.backend.global.payload.status.ReasonDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneralException extends RuntimeException {

    private final BaseStatus status;

    public ReasonDto getErrorReasonHttpStatus(){
        return this.status.getReasonHttpStatus();
    }
}
