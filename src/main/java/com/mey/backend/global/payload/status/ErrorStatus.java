package com.mey.backend.global.payload.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {
    // 가장 일반적인 응답
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의 바랍니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON_400","잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON_401","인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "금지된 요청입니다."),

    // Auth Error
    USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "AUTH_4001", "이미 존재하는 사용자명입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "AUTH_4002", "이미 존재하는 이메일입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_4011", "유효하지 않은 리프레시 토큰입니다."),

    // Member Error
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_4001", "사용자를 찾을 수 없습니다."),

    // Place Error
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_4001", "해당 장소를 찾을 수 없습니다."),

    // Route Error
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_4001", "해당 루트를 찾을 수 없습니다."),

    // UserRoute Error
    USER_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_ROUTE_4001", "해당 사용자 루트를 찾을 수 없습니다."),
    ROUTE_ALREADY_SAVED(HttpStatus.BAD_REQUEST, "USER_ROUTE_4002", "이미 저장된 루트입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .httpStatus(httpStatus)
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }
}
