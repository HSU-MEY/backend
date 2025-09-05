package com.mey.backend.domain.route.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitStepDto {

    public enum Mode { WALK, BUS, SUBWAY, RAIL, TAXI }

    private Mode mode;                 // 단계 이동 수단
    private String instruction;        // 안내 문구 (예: "정류장까지 120m 이동")
    private Integer distanceMeters;    // 단계 거리(더미)
    private Integer durationSeconds;   // 단계 소요(더미)

    // 대중교통일 경우에만
    private String lineName;           // 노선명 (예: "273")
    private String headsign;           // 행선지 (예: "강남역 방면")
    private Integer numStops;          // 정거장 수

    // 지도를 위한 간단 폴리라인(위경도 배열) — 더미
    private List<LatLngDto> polyline;
}
