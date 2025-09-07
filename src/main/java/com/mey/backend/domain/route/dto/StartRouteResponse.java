package com.mey.backend.domain.route.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StartRouteResponse {
    private List<TransitSegmentDto> segments;
}