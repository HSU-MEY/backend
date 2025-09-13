package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class PlaceThemeResponseDto {
    private Long id;
    private String nameKo;
    private String nameEn;
    private String nameJp;
    private String nameCh;
    private List<String> themes;
    private String imageUrl;
    private Map<String, String> openingHours;
    private String addressKo;
    private String addressEn;
    private String addressJp;
    private String addressCh;

    public PlaceThemeResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.nameKo = place.getNameKo();
        this.nameEn = place.getNameEn();
        this.nameJp = place.getNameJp();
        this.nameCh = place.getNameCh();
        this.themes = place.getThemes();
        this.imageUrl = place.getImageUrl();
        this.openingHours = place.getOpeningHours();
        this.addressKo = place.getAddressKo();
        this.addressEn = place.getAddressEn();
        this.addressJp = place.getAddressJp();
        this.addressCh = place.getAddressCh();
    }
}