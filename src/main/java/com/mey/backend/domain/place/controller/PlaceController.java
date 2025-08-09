package com.mey.backend.domain.place.controller;

import com.mey.backend.domain.place.dto.PlaceResponseDto;
import com.mey.backend.domain.place.dto.PlaceThemeResponseDto;
import com.mey.backend.domain.place.service.PlaceService;
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

    @GetMapping("/search")
    public List<PlaceResponseDto> searchPlaces(@RequestParam String keyword) {
        return placeService.searchPlaces(keyword);
    }

    @GetMapping("/{placeId}")
    public PlaceResponseDto getPlaceDetail(@PathVariable Long placeId) {
        return placeService.getPlaceDetail(placeId);
    }

    @GetMapping("/popular")
    public List<PlaceResponseDto> getPopularPlaces() {
        return placeService.getPopularPlaces();
    }

    @GetMapping("/theme")
    public List<PlaceThemeResponseDto> getPlacesByTheme(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit // 기본값 1
    ) {
        return placeService.getPlacesByTheme(keyword, limit);
    }
}