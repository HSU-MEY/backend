package com.mey.backend.domain.itinerary.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoordinateDto {
    private Double lat;
    private Double lng;

    // 선택 필드: 프론트에서 붙여 올 수도 있는 정보
    private Long placeId;     // 내부 Place와 연결 시
    private String name;      // 화면용 별칭
    private Integer originalIndex; // 프론트가 보낸 원래 순번(없으면 서버가 0..N-1로 부여)
}