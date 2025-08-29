package com.mey.backend.domain.route.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartRouteResponseDto {
    private Long routeId;
    private Integer totalDistanceMeters;     // 전체 합계(더미)
    private Integer totalDurationSeconds;    // 전체 합계(더미)
    private Integer totalFare;               // 전체 요금 합계(더미, 원화 가정)
    private String currency;                 // "KRW" 등

    private List<TransitSegmentDto> segments; // 현재→1, 1→2, 2→3 ...
}
