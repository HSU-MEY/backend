package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.Getter;

@Getter
public class PlaceSimpleResponseDto {

    private Long id;
    private Double longitude;
    private Double latitude;
    private Long regionId;

    public PlaceSimpleResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.longitude = place.getLongitude();
        this.latitude = place.getLatitude();
        this.regionId = place.getRegion() != null ? place.getRegion().getRegionId() : null;
    }
}