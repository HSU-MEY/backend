package com.mey.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}