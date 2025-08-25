package com.mey.backend.domain.itinerary.controller;

import com.mey.backend.domain.itinerary.dto.CreateItineraryRequest;
import com.mey.backend.domain.itinerary.dto.CreateItineraryResponse;
import com.mey.backend.domain.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "여정", description = "여정 생성 및 반환 관련 API")
@RestController
@RequestMapping("/api/itineray")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @Operation(summary = "여정 생성", description = "여정을 생성하고 반환합니다")
    // POST /api/itineraries : 좌표 N개를 받아 순서를 산출하고 저장 + 응답
    @PostMapping
    public CreateItineraryResponse create(@Validated @RequestBody CreateItineraryRequest request) {

        return itineraryService.createItinerary(request);

    }
}
