package com.mey.backend.domain.place.dto;

import com.mey.backend.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlaceThemeResponseDto {
    private Long id;
    private String nameKo;
    private List<String> themes;

    public PlaceThemeResponseDto(Place place) {
        this.id = place.getPlaceId();
        this.nameKo = place.getNameKo();
        this.themes = place.getThemes();
    }
}