package com.mey.backend.domain.itinerary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateItineraryRequest {
    @Min(2)
    private int placeCount;

    @NotNull @NotEmpty
    private List<CoordinateDto> places; // 길이 == placeCount 여야 함

    // 향후 옵션(출발점 고정, 시간제약, 교통수단 선호 등) 추가 가능
}