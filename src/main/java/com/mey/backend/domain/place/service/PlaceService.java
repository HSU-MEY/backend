package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceSimpleResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.place.repository.UserLikePlaceRepository;
import com.mey.backend.domain.region.entity.Region;
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
        Region seoul = new Region(1L, "서울", "Seoul");
        Region busan = new Region(2L, "부산", "Busan");
        Region jeju = new Region(3L, "제주", "Jeju");

        Place dummy1 = Place.builder()
                .placeId(1L)
                .region(seoul)
                .nameKo("경복궁")
                .nameEn("Gyeongbokgung Palace")
                .descriptionKo("조선의 대표 궁궐")
                .descriptionEn("Main royal palace of the Joseon dynasty")
                .longitude(126.9769)
                .latitude(37.5796)
                .build();

        Place dummy2 = Place.builder()
                .placeId(2L)
                .region(seoul)
                .nameKo("남산타워")
                .nameEn("Namsan Tower")
                .descriptionKo("서울의 랜드마크 전망대")
                .descriptionEn("Iconic observation tower in Seoul")
                .longitude(126.9882)
                .latitude(37.5512)
                .build();

        Place dummy3 = Place.builder()
                .placeId(3L)
                .region(seoul)
                .nameKo("동대문디자인플라자")
                .nameEn("Dongdaemun Design Plaza")
                .descriptionKo("서울의 현대 건축 랜드마크")
                .descriptionEn("Modern architectural landmark in Seoul")
                .longitude(127.0095)
                .latitude(37.5663)
                .build();

        Place dummy4 = Place.builder()
                .placeId(4L)
                .region(busan)
                .nameKo("해운대 해수욕장")
                .nameEn("Haeundae Beach")
                .descriptionKo("부산의 대표 해수욕장")
                .descriptionEn("Most famous beach in Busan")
                .longitude(129.1604)
                .latitude(35.1587)
                .build();

        Place dummy5 = Place.builder()
                .placeId(5L)
                .region(jeju)
                .nameKo("성산일출봉")
                .nameEn("Seongsan Ilchulbong")
                .descriptionKo("제주의 일출 명소")
                .descriptionEn("Famous sunrise spot in Jeju")
                .longitude(126.9420)
                .latitude(33.4580)
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