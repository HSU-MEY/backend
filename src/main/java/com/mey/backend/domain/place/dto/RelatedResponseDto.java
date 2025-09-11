package com.mey.backend.domain.place.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelatedResponseDto {

    private String rlteTatsNm;       // 연관관광지명
    private String rlteRegnNm;       // 연관관광지 시도명
    private String rlteSignguNm;     // 연관관광지 시군구명
    private String rlteCtgryLclsNm;  // 대분류
    private String rlteCtgryMclsNm;  // 중분류
    private String rlteCtgrySclsNm;  // 소분류
    private Double distanceMeter;    // 출발지→연관관광지 직선거리(m)
}
