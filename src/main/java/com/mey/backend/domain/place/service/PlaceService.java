package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceSimpleResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.place.repository.UserLikePlaceRepository;
import com.mey.backend.global.exception.PlaceException;
import com.mey.backend.global.payload.status.ErrorStatus;
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
                .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
        return new PlaceResponseDto(place);
    }

    public PlaceSimpleResponseDto getPlaceByName(String nameKo) {
        Place place = placeRepository.findByNameKo(nameKo)
                .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
        return new PlaceSimpleResponseDto(place);
    }

    public List<PlaceResponseDto> getPopularPlaces() {
        List<Place> places = userLikePlaceRepository.findPopularPlaces();
        if (places.isEmpty()) {
            throw new PlaceException(ErrorStatus.PLACE_NOT_FOUND);
        }

        return places.stream()
                .map(PlaceResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<PlaceResponseDto> getPopularPlaces2() {
        Place dummy1 = Place.builder()
                .placeId(1L)
                .nameKo("경복궁")
                .nameEn("Gyeongbokgung Palace")
                .descriptionKo("조선의 대표 궁궐")
                .descriptionEn("Main royal palace of the Joseon dynasty")
                .longitude(126.9769)
                .latitude(37.5796)
                .build();

        Place dummy2 = Place.builder()
                .placeId(2L)
                .nameKo("남산타워")
                .nameEn("Namsan Tower")
                .descriptionKo("서울의 랜드마크 전망대")
                .descriptionEn("Iconic observation tower in Seoul")
                .longitude(126.9882)
                .latitude(37.5512)
                .build();

        Place dummy3 = Place.builder()
                .placeId(3L)
                .nameKo("명동거리")
                .nameEn("Myeongdong Street")
                .descriptionKo("서울의 대표 쇼핑 거리")
                .descriptionEn("Famous shopping district in Seoul")
                .longitude(126.9850)
                .latitude(37.5636)
                .build();

        Place dummy4 = Place.builder()
                .placeId(4L)
                .nameKo("북촌한옥마을")
                .nameEn("Bukchon Hanok Village")
                .descriptionKo("전통 한옥이 모여있는 마을")
                .descriptionEn("Traditional Hanok village in Seoul")
                .longitude(126.9849)
                .latitude(37.5826)
                .build();

        Place dummy5 = Place.builder()
                .placeId(5L)
                .nameKo("동대문디자인플라자")
                .nameEn("Dongdaemun Design Plaza")
                .descriptionKo("서울의 현대 건축 랜드마크")
                .descriptionEn("Modern architectural landmark in Seoul")
                .longitude(127.0095)
                .latitude(37.5663)
                .build();

        return List.of(
                new PlaceResponseDto(dummy1),
                new PlaceResponseDto(dummy2),
                new PlaceResponseDto(dummy3),
                new PlaceResponseDto(dummy4),
                new PlaceResponseDto(dummy5)
        );
    }


    public List<PlaceThemeResponseDto> getPlacesByTheme(String keyword, int limit) {
        String jsonKeyword = "[\"" + keyword.toLowerCase() + "\"]";
        List<Place> places = placeRepository.findByThemeKeywordWithLimit(jsonKeyword, limit);

        if (places.isEmpty()) {
            throw new PlaceException(ErrorStatus.PLACE_NOT_FOUND);
        }

        return places.stream()
                .map(PlaceThemeResponseDto::new)
                .collect(Collectors.toList());
    }
}