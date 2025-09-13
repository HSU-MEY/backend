package com.mey.backend.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDetailResponseDto {
    
    private Long routeId;
    private String title;
    private String description;
    private String theme;
    private String imageUrl;
    private BigDecimal totalDistanceKm;
    private Integer totalDurationMinutes;
    private Integer estimatedCost;
    private List<LocalTime> suggestedStartTimes;
    private List<RoutePlaceDto> routePlaces;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePlaceDto {
        private Integer sequenceOrder;
        private PlaceDto place;
        private Integer recommendedDurationMinutes;
        private LocalTime estimatedArrivalTime;
        private LocalTime estimatedDepartureTime;
        private String notes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDto {
        private Long placeId;
        private String name;
        private String description;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String address;
        private List<String> imageUrls;
        private Map<String, String> openingHours;
    }

}