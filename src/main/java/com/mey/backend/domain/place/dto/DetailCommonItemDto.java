package com.mey.backend.domain.place.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DetailCommonItemDto {
    String contentId;
    String title;
    String overview;
    String addr;
    String mapx;
    String mapy;
    String areaCode;
    String siGunGuCode;
    String image;
}

