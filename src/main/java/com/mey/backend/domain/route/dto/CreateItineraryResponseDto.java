package com.mey.backend.domain.route.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateItineraryResponseDto {
    private Long itineraryId;
    private List<OrderedStopDto> orderedStops;
    private int totalDistanceMeters;
}
