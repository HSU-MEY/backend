package com.mey.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "사용자 정보 수정 요청")
public class UserInfoUpdateRequest {
    
    @Schema(description = "닉네임", example = "새닉네임")
    private String nickname;
    
    @Schema(description = "비밀번호", example = "newpassword123")
    private String password;
    
    @Schema(description = "이메일 (수정 시 재로그인 필수)", example = "newemail@example.com")
    private String email;
}
