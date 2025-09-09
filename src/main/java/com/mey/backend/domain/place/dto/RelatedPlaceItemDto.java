package com.mey.backend.domain.place.dto;

import lombok.Value;

@Value
public class RelatedPlaceItemDto {
    String rlteTatsCd;
    String rlteTatsNm;   // 연관 관광지 이름
}
