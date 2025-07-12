package com.mey.backend.domain.user.controller;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.service.UserService;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.security.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "현재 사용자 정보 조회 (테스트 목적)",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public CommonResponse<UserInfoResponse> getCurrentUserInfo(@CurrentUser User user) {
        UserInfoResponse userInfo = userService.getUserInfo(user);
        return CommonResponse.onSuccess(userInfo);
    }
}
