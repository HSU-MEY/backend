package com.mey.backend.domain.user.controller;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.dto.UserInfoUpdateRequest;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.service.UserService;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.security.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 정보 조회",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/profiles")
    public CommonResponse<UserInfoResponse> getCurrentUserInfo(@Parameter(hidden = true) @CurrentUser User user) {
        UserInfoResponse userInfo = userService.getUserInfo(user);
        return CommonResponse.onSuccess(userInfo);
    }

    @Operation(
            summary = "사용자 정보 수정",
            description = "JWT 토큰으로 인증된 현재 사용자의 정보 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(value = "/profiles", consumes = "multipart/form-data")
    public CommonResponse<Void> updateProfile(@Parameter(hidden = true) @CurrentUser User user,
                                              @RequestPart("request") UserInfoUpdateRequest request,
                                              @RequestPart("profileImage") MultipartFile profileImage) {
        userService.updateUserInfo(user, request, profileImage);
        return CommonResponse.onSuccess(null);
    }
}
