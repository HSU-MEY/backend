package com.mey.backend.domain.route.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitMetricsDto {
    private int distanceMeters;   // 구간 총 거리
    private int durationSeconds;  // 구간 총 소요시간
    private int fare;             // 구간 요금
}