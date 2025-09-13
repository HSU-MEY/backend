package com.mey.backend.domain.user.dto;

import com.mey.backend.domain.user.entity.Role;
import com.mey.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString(exclude = {"email"})
public class UserInfoResponse {
    private Long id;
    private String nickname;
    private String email;
    private Role role;
    private String profileImageUrl;

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getProfileImageUrl()
        );
    }
}