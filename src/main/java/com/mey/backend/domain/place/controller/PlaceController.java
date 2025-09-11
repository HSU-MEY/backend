package com.mey.backend.domain.place.controller;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceSimpleResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.dto.RelatedResponseDto;
import com.mey.backend.domain.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장소", description = "장소 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @Operation(
            summary = "장소 이름으로 장소 검색",
            description = "장소 이름로 장소를 검색한 결과를 반환합니다."
    )
    @GetMapping("/search")
    public List<PlaceSimpleResponseDto> searchPlaces(@RequestParam String keyword) {
        return placeService.searchPlaces(keyword);
    }

    @Operation(
            summary = "장소 ID로 장소 검색",
            description = "장소 ID로 장소를 검색한 결과를 반환합니다."
    )
    @GetMapping("/{placeId}")
    public PlaceResponseDto getPlaceDetail(@PathVariable Long placeId) {

        return placeService.getPlaceDetail(placeId);
    }

    @Operation(
            summary = "장소 ID로 연계 관광지 정보 조회",
            description = "장소 ID로 연계 관광지 정보를 조회한 결과를 반환합니다."
    )
    @GetMapping("/{placeId}/related/{language}")
    public List<RelatedResponseDto> getRelatedPlaces(@PathVariable Long placeId, @PathVariable String language) {

        return placeService.getRelatedPlaces(placeId, language);
    }

    @Operation(
            summary = "인기 장소 조회",
            description = "인기 장소 리스트를 조회한 결과를 반환합니다."
    )
    @GetMapping("/popular")
    public List<PlaceResponseDto> getPopularPlaces(
            @RequestParam(defaultValue = "10") int limit) {
        return placeService.getPopularPlaces(limit);
    }

    @Operation(
            summary = "장소 테마로 장소 검색",
            description = "장소 테마로 장소를 검색한 결과를 반환합니다."
    )
    @GetMapping("/theme")
    public List<PlaceThemeResponseDto> getPlacesByTheme(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return placeService.getPlacesByTheme(keyword, limit);
    }
}
