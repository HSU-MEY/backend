package com.mey.backend.domain.itinerary.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItinerarySummaryDto {
    private Integer totalDistanceMeters;   // 추정치 or GPT 요약값 (없으면 null)
    private Integer totalDurationSeconds;  // 추정치 or GPT 요약값
    private Integer totalCost;             // 원화 등 (없으면 null)
    private String summaryText;            // "총 Xkm, 약 Y분" 같은 자유 요약문
}