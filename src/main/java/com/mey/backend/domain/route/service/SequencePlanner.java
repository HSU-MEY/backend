package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.CoordinateDto;
import java.util.List;

@FunctionalInterface
public interface SequencePlanner {
    PlanResult plan(List<CoordinateDto> points);

    // 표준 반환 형태
    record PlanResult(
            List<Integer> order,          // 방문 순서: 입력 원본 인덱스 리스트
            int totalDistanceMeters
    ) {}
}

