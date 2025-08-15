package com.mey.backend.domain.route.dto;

import com.mey.backend.domain.route.entity.RouteType;
import com.mey.backend.domain.route.entity.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루트 생성 요청 DTO (테스트 전용)")
public class RouteCreateRequestDto {

    @Schema(description = "지역 ID", example = "1")
    private Long regionId;

    @Schema(description = "루트명 (한국어)", example = "테스트 루트")
    private String titleKo;

    @Schema(description = "루트명 (영어)", example = "Test Route")
    private String titleEn;

    @Schema(description = "루트 설명 (한국어)", example = "테스트용으로 생성된 루트입니다")
    private String descriptionKo;

    @Schema(description = "루트 설명 (영어)", example = "This is a test route")
    private String descriptionEn;

    @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "비용", example = "50000")
    private int cost;

    @Schema(description = "총 소요시간 (분)", example = "240")
    private int totalDurationMinutes;

    @Schema(description = "총 거리 (km)", example = "5.5")
    private double totalDistance;

    @Schema(description = "총 예상 비용", example = "30000")
    private double totalCost;

    @Schema(description = "테마 목록")
    private List<Theme> themes;

    @Schema(description = "루트 타입")
    private RouteType routeType;
}