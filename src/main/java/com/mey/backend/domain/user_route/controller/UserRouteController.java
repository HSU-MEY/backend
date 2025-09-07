package com.mey.backend.domain.user_route.controller;

import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user_route.dto.*;
import com.mey.backend.domain.user_route.entity.UserRouteStatus;
import com.mey.backend.domain.user_route.service.UserRouteService;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.security.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 루트", description = "사용자 루트 관련 API")
@RestController
@RequestMapping("/api/users/routes")
@RequiredArgsConstructor
public class UserRouteController {

    private final UserRouteService userRouteService;

    @Operation(
            summary = "유저 루트 저장",
            description = "사용자가 선택한 루트를 저장",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
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
    @GetMapping
    public CommonResponse<UserRouteListResponseDto> getUserRoutes(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestParam(required = false) UserRouteStatus status) {
        UserRouteListResponseDto response = userRouteService.getUserRoutes(user, status);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "유저 루트 수정",
            description = "사용자가 저장한 루트의 일정 수정",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{savedRouteId}")
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
    @DeleteMapping("/{savedRouteId}")
    public CommonResponse<Void> deleteUserRoute(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable Long savedRouteId) {
        userRouteService.deleteUserRoute(user, savedRouteId);
        return CommonResponse.onSuccess(null);
    }
}