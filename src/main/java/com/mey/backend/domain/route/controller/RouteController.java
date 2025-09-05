package com.mey.backend.domain.route.controller;

import com.mey.backend.domain.route.dto.*;
import com.mey.backend.domain.route.entity.Theme;
import com.mey.backend.domain.route.service.RouteService;
import com.mey.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "루트", description = "루트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    @Operation(
            summary = "루트 생성 (테스트 전용)",
            description = "테스트 목적으로 새로운 루트를 생성합니다. 실제 운영 환경에서는 사용하지 마세요."
    )
    @PostMapping
    public CommonResponse<RouteCreateResponseDto> createRoute(@RequestBody RouteCreateRequestDto request) {
        RouteCreateResponseDto response = routeService.createRoute(request);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "AI가이드 추천 루트 생성",
            description = "AI가이드 추천 루트를 생성하고 결과를 반환합니다."
    )
    @PostMapping("/ai-recommend")
    public CommonResponse<RouteCreateResponseDto> createByAI(
            @Validated @RequestBody CreateRouteByPlaceIdsRequestDto request) {
        RouteCreateResponseDto response = routeService.createRouteByAI(request);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "루트 시작",
            description = "현재 위치와 루트 ID를 받아 현재 → 1번째, i번째 → i+1번째 구간의 단계별 길찾기 정보를 반환합니다."
    )
    @PostMapping("/{route_id}/start")
    public CommonResponse<StartRouteResponse> startRoute(
            @PathVariable("route_id") Long routeId,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        StartRouteResponse response = routeService.startRoute(routeId, latitude, longitude);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "추천 루트 조회",
            description = "추천 루트들을 조회한 결과를 반환합니다."
    )
    @GetMapping("/recommend")
    public CommonResponse<RouteRecommendListResponseDto> getRecommendedRoutes(
            @RequestParam(required = false) List<Theme> themes,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        RouteRecommendListResponseDto response = routeService.getRecommendedRoutes(themes, region, limit, offset);
        return CommonResponse.onSuccess(response);
    }

    @Operation(
            summary = "루트 아이디로 루트 검색",
            description = "루트 아이디로 루트를 검색한 결과를 반환합니다."
    )
    @GetMapping("/{routeId}")
    public CommonResponse<RouteDetailResponseDto> getRouteDetail(
            @PathVariable Long routeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime) {

        RouteDetailResponseDto response = routeService.getRouteDetail(routeId, date, startTime);
        return CommonResponse.onSuccess(response);
    }
}
