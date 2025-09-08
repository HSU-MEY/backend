package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class PlaceResponseDto {

    private Long id;
    private Long regionId;

    private String nameKo;
    private String nameEn;

    private String descriptionKo;
    private String descriptionEn;

    private Double longitude;
    private Double latitude;

    private String imageUrl;
    private String address;
    private String contactInfo;
    private String websiteUrl;

    private String kakaoPlaceId;
    private String tourApiPlaceId;

    private Map<String, String> openingHours;
    private List<String> themes;

    private String costInfo;

    public PlaceResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.regionId = place.getRegion() != null ? place.getRegion().getRegionId() : null;
        this.nameKo = place.getNameKo();
        this.nameEn = place.getNameEn();
        this.descriptionKo = place.getDescriptionKo();
        this.descriptionEn = place.getDescriptionEn();
        this.longitude = place.getLongitude();
        this.latitude = place.getLatitude();
        this.imageUrl = place.getImageUrl();
        this.address = place.getAddress();
        this.contactInfo = place.getContactInfo();
        this.websiteUrl = place.getWebsiteUrl();
        this.kakaoPlaceId = place.getKakaoPlaceId();
        this.tourApiPlaceId = place.getTourApiPlaceId();
        this.openingHours = place.getOpeningHours();
        this.themes = place.getThemes();
        this.costInfo = place.getCostInfo();
    }
}