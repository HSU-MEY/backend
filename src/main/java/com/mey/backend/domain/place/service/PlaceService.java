package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.place.repository.UserLikePlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UserLikePlaceRepository userLikePlaceRepository;

    public List<PlaceResponseDto> searchPlaces(String keyword) {
        return placeRepository.findByNameKoContainingIgnoreCase(keyword).stream()
                .map(PlaceResponseDto::new)
                .collect(Collectors.toList());
    }

    public PlaceResponseDto getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장소를 찾을 수 없습니다."));
        return new PlaceResponseDto(place);
    }

    public List<PlaceResponseDto> getPopularPlaces() {
        return userLikePlaceRepository.findPopularPlaces().stream()
                .map(PlaceResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<PlaceThemeResponseDto> getPlacesByTheme(String keyword, int limit) {
        String jsonKeyword = "[\"" + keyword.toLowerCase() + "\"]";
        List<Place> places = placeRepository.findByThemeKeywordWithLimit(jsonKeyword, limit);

        return places.stream()
                .map(PlaceThemeResponseDto::new)
                .collect(Collectors.toList());
    }
}