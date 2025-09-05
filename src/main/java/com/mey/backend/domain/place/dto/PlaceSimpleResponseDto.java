package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.Getter;

@Getter
public class PlaceSimpleResponseDto {

    private Long id;
    private String nameKo;
    private String nameEn;
    private Double longitude;
    private Double latitude;
    private Long regionId;

    public PlaceSimpleResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.nameKo = place.getNameKo();
        this.nameEn = place.getNameEn();
        this.longitude = place.getLongitude();
        this.latitude = place.getLatitude();
        this.regionId = place.getRegion().getRegionId();
    }
}