package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceSimpleResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.dto.RelatedResponseDto;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.global.exception.PlaceException;
import com.mey.backend.global.payload.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceTourApiClient tourApiClient;

    public List<PlaceSimpleResponseDto> searchPlaces(String keyword) {

        return placeRepository
                .findByNameKoContainingIgnoreCaseOrNameEnContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(PlaceSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    public PlaceResponseDto getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
        return new PlaceResponseDto(place);
    }

    public List<RelatedResponseDto> getRelatedPlaces(Long placeId) {

        Place place = placeRepository.findPlaceByPlaceId(placeId);

        String[] getAreaCdSiGunGuCd = tourApiClient.fetchRegionCodesByLocation(place.getLatitude(), place.getLongitude());

        return tourApiClient.fetchRelatedPlacesInfo(
                place,
                "202508",
                getAreaCdSiGunGuCd[0],
                getAreaCdSiGunGuCd[1]
        );
    }

    @Transactional(readOnly = true)
    public List<PlaceResponseDto> getPopularPlaces(Integer limit) {
        int n = (limit == null || limit <= 0) ? 10 : limit;

        // placeId 오름차순으로 홀수 ID만 2n개 정도 가져오기
        Pageable pageable = PageRequest.of(0, 2 * n, Sort.by(Sort.Direction.ASC, "placeId"));
        List<Place> places = placeRepository.findOddIdPlaces(pageable);

        // 최종 반환은 최대 n개까지만 보장
        return places.stream()
                .limit(n)
                .map(PlaceResponseDto::new)
                .toList();
    }

    public List<PlaceThemeResponseDto> getPlacesByTheme(String keyword, int limit) {
        // DB 저장 형식에 맞추어 정규화 (예: 전부 대문자 + 언더스코어)
        String norm = keyword.toUpperCase(); // "K_POP", "K_DRAMA" 등

        List<Place> places = placeRepository.findByThemeKeywordWithLimit(norm, limit);
        if (places.isEmpty()) throw new PlaceException(ErrorStatus.PLACE_NOT_FOUND);

        return places.stream().map(PlaceThemeResponseDto::new).collect(Collectors.toList());
    }
}