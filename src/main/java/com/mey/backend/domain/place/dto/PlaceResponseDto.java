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
    private String nameJp;
    private String nameCh;

    private String descriptionKo;
    private String descriptionEn;
    private String descriptionJp;
    private String descriptionCh;

    private Double longitude;
    private Double latitude;

    private String imageUrl;

    private String addressKo;
    private String addressEn;
    private String addressJp;
    private String addressCh;

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
        this.nameJp = place.getNameJp();
        this.nameCh = place.getNameCh();
        this.descriptionKo = place.getDescriptionKo();
        this.descriptionEn = place.getDescriptionEn();
        this.descriptionJp = place.getDescriptionJp();
        this.descriptionCh = place.getDescriptionCh();
        this.longitude = place.getLongitude();
        this.latitude = place.getLatitude();
        this.imageUrl = place.getImageUrl();
        this.addressKo = place.getAddressKo();
        this.addressEn = place.getAddressEn();
        this.addressJp = place.getAddressJp();
        this.addressCh = place.getAddressCh();
        this.contactInfo = place.getContactInfo();
        this.websiteUrl = place.getWebsiteUrl();
        this.kakaoPlaceId = place.getKakaoPlaceId();
        this.tourApiPlaceId = place.getTourApiPlaceId();
        this.openingHours = place.getOpeningHours();
        this.themes = place.getThemes();
        this.costInfo = place.getCostInfo();
    }
}