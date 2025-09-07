package com.mey.backend.domain.route.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitSegmentDto {

    // 구간 정보
    private String fromName;
    private Double fromLat;
    private Double fromLng;

    private String toName;
    private Double toLat;
    private Double toLng;

    // 요약
    private Integer distanceMeters;       // 이 구간 총 거리(더미)
    private Integer durationSeconds;      // 이 구간 총 소요(더미)
    private Integer fare;                 // 이 구간 요금(더미)
    private String summary;               // "도보 5분 → 버스 3정거장 → 도보 2분" 같은 짧은 요약

    // 단계별 안내(도보/버스/지하철 등)
    private List<TransitStepDto> steps;
}
