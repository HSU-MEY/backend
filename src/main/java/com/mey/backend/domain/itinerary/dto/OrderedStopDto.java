package com.mey.backend.domain.itinerary.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderedStopDto {
    private int order;            // 이곳이 방문 순서상 몇 번째인지 명시
    private int originalIndex;    // 프론트가 보낸 원래 순서의 인덱스
    private CoordinateDto coord;
}