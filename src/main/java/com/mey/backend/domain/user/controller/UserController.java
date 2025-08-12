package com.mey.backend.domain.user.controller;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.dto.UserInfoUpdateRequest;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.service.UserService;
import com.mey.backend.domain.user_route.dto.*;
import com.mey.backend.domain.user_route.service.UserRouteService;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.security.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRouteService userRouteService;

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
    @PutMapping("/profiles")
    public CommonResponse<Void> updateProfile(@Parameter(hidden = true) @CurrentUser User user,
                                              @RequestBody UserInfoUpdateRequest request) {
        userService.updateUserInfo(user, request);
        return CommonResponse.onSuccess(null);
    }

    @Operation(
            summary = "유저 루트 저장",
            description = "사용자가 선택한 루트를 저장",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/routes")
    public CommonResponse<UserRouteSaveResponseDto> saveUserRoute(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody UserRouteSaveRequestDto request) {
        UserRouteSaveResponseDto response = userRouteService.saveUserRoute(user, request);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "유저 루트 목록 조회",
            description = "사용자가 저장한 루트 목록 조회",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/routes")
    public CommonResponse<UserRouteListResponseDto> getUserRoutes(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestParam(required = false) String status) {
        UserRouteListResponseDto response = userRouteService.getUserRoutes(user, status);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "유저 루트 수정",
            description = "사용자가 저장한 루트의 일정 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/routes/{savedRouteId}")
    public CommonResponse<Void> updateUserRoute(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long savedRouteId,
            @RequestBody UserRouteUpdateRequestDto request) {
        userRouteService.updateUserRoute(user, savedRouteId, request);
        return CommonResponse.onSuccess(null);
    }

    @Operation(
            summary = "유저 루트 삭제",
            description = "사용자가 저장한 루트 삭제",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/routes/{savedRouteId}")
    public CommonResponse<Void> deleteUserRoute(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long savedRouteId) {
        userRouteService.deleteUserRoute(user, savedRouteId);
        return CommonResponse.onSuccess(null);
    }

}
