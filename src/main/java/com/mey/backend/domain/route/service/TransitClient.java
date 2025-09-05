package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.TransitMetricsDto;
import com.mey.backend.domain.route.dto.TransitSegmentDto;
import java.time.LocalDateTime;

public interface TransitClient {

    // 구현체가 반드시 구현해야 할 메서드 (이름 포함)
    TransitSegmentDto route(
            String fromName, double fromLat, double fromLng,
            String toName,   double toLat,   double toLng,
            LocalDateTime departureTime
    );

    // 편의용: 이름 없이 좌표만 넘기면 빈 이름으로 위 메서드 호출
    default TransitSegmentDto route(
            double fromLat, double fromLng,
            double toLat,   double toLng,
            LocalDateTime departureTime
    ) {
        return route("", fromLat, fromLng, "", toLat, toLng, departureTime);
    }
    default TransitMetricsDto metrics(
            double fromLat, double fromLng, double toLat, double toLng, LocalDateTime departureTime
    ) {
        // 기본 구현: route()를 불러 합계만 뽑아 쓰기 (구현체에서 오버라이드하면 step 파싱 자체를 생략)
        TransitSegmentDto seg = route(fromLat, fromLng, toLat, toLng, departureTime);
        return TransitMetricsDto.builder()
                .distanceMeters(seg.getDistanceMeters() != null ? seg.getDistanceMeters() : 0)
                .durationSeconds(seg.getDurationSeconds() != null ? seg.getDurationSeconds() : 0)
                .fare(seg.getFare() != null ? seg.getFare() : 0)
                .build();
    }
}