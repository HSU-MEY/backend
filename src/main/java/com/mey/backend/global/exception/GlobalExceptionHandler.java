package com.mey.backend.global.exception;

import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.payload.status.ErrorStatus;
import com.mey.backend.global.payload.status.ReasonDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "ConstraintViolationException이 ConstraintViolation을 포함하지 않습니다."));

        ErrorStatus errorStatus = ErrorStatus.BAD_REQUEST;
        for (ErrorStatus value : ErrorStatus.values()) {
            if (value.name().equals(errorMessage)) {
                errorStatus = value;
                break;
            }
        }

        CommonResponse<Object> body = CommonResponse.onFailure(errorStatus.getCode(),
                errorStatus.getMessage(),
                null);

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY, errorStatus.getHttpStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors()
                .forEach(fieldError -> {
                    String fieldName = fieldError.getField();
                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                    errors.merge(fieldName, errorMessage,
                            (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
                });

        CommonResponse<Object> body = CommonResponse.onFailure(ErrorStatus.BAD_REQUEST.getCode(),
                ErrorStatus.BAD_REQUEST.getMessage(),
                errors);

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY, ErrorStatus.BAD_REQUEST.getHttpStatus(), request);
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> handleGeneral(GeneralException generalException, HttpServletRequest request) {
        ReasonDto errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();

        CommonResponse<Object> body = CommonResponse.onFailure(errorReasonHttpStatus.getCode(),
                errorReasonHttpStatus.getMessage(),
                null);

        return super.handleExceptionInternal(generalException, body, null, errorReasonHttpStatus.getHttpStatus(),
                new ServletWebRequest(request));
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleOthers(Exception e, WebRequest request) {
        CommonResponse<Object> body = CommonResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR.getCode(),
                ErrorStatus.INTERNAL_SERVER_ERROR.getMessage(),
                e.getMessage());

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY,
                ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus(), request);
    }
}
