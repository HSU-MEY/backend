package com.mey.backend.domain.route.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinateDto {
    private Double lat;
    private Double lng;
    private Integer originalIndex; // 프론트가 보낸 원래 순번(없으면 서버가 0..N-1로 부여)
    private Long placeId;
    private String name;       // 한국어 이름
    private String addressKo;  // 한국 주소
}
