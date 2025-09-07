package com.mey.backend.domain.auth.controller;

import com.mey.backend.domain.auth.dto.LoginRequest;
import com.mey.backend.domain.auth.dto.SignupRequest;
import com.mey.backend.domain.auth.dto.TokenResponse;
import com.mey.backend.domain.auth.service.AuthService;
import com.mey.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새 사용자를 등록합니다")
    @PostMapping("/signup")
    public CommonResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return CommonResponse.onSuccess(null);
    }

    @Operation(summary = "로그인", description = "사용자 로그인을 처리합니다")
    @PostMapping("/login")
    public CommonResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return CommonResponse.onSuccess(tokenResponse);
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다")
    @PostMapping("/refresh")
    public CommonResponse<TokenResponse> refreshToken(@RequestParam String refreshToken) {
        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        return CommonResponse.onSuccess(tokenResponse);
    }
}
