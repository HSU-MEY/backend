package com.mey.backend.global.payload;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseStatusConverter implements ResponseBodyAdvice<CommonResponse<?>> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return returnType.getParameterType() == CommonResponse.class;
    }

    @Override
    public CommonResponse<?> beforeBodyWrite(
            CommonResponse<?> body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        HttpStatus status = body.getHttpStatus();
        if (status != null) {
            response.setStatusCode(status);
        }

        return body;
    }
}
