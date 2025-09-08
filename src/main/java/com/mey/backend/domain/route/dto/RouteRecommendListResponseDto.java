package com.mey.backend.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendListResponseDto {
    
    private List<RouteRecommendResponseDto> routes;
    private Integer totalCount;
}