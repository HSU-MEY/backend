package com.mey.backend.domain.user_route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteListResponseDto {
    
    private List<SavedRouteDto> savedRoutes;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedRouteDto {
        private Long savedRouteId;
        private Long routeId;
        private String title;
        private String description;
        private String imageUrl;
        private Integer totalDurationMinutes;
        private LocalDate preferredStartDate;
        private LocalTime preferredStartTime;
        private Boolean isPastDate;
        private Integer daysUntilTrip;
        private LocalDateTime savedAt;
    }
}