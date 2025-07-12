package com.mey.backend.domain.user.dto;

import com.mey.backend.domain.user.entity.Role;
import com.mey.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}