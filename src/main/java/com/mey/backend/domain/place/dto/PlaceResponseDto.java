package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.Getter;

@Getter
public class PlaceResponseDto {

    private Long id;
    private String nameKo;
    private String nameEn;
    private String descriptionKo;
    private String descriptionEn;
    private Double longitude;
    private Double latitude;
    private Long regionId;

    public PlaceResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.nameKo = place.getNameKo();
        this.nameEn = place.getNameEn();
        this.descriptionKo = place.getDescriptionKo();
        this.descriptionEn = place.getDescriptionEn();
        this.longitude = place.getLongitude();
        this.latitude = place.getLatitude();
        this.regionId = place.getRegion() != null ? place.getRegion().getRegionId() : null;
    }
}