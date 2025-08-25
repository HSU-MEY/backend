package com.mey.backend.domain.itinerary.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateItineraryResponse {
    private Long itineraryId;
    private List<OrderedStopDto> orderedStops;
    private ItinerarySummaryDto summary;
}