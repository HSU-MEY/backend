package com.mey.backend.domain.user.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoUpdateRequest {
    // private String username;
    private String password;
    private String email;
}