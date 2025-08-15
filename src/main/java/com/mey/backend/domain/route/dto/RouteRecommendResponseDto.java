package com.mey.backend.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendResponseDto {
    
    private Long routeId;
    private String title;
    private String description;
    private String theme;
    private BigDecimal totalDistanceKm;
    private Integer totalDurationMinutes;
    private Integer estimatedCost;
    private String thumbnailUrl;
    private Integer popularityScore;
    private List<LocalTime> availableTimes;
}