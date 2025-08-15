package com.mey.backend.domain.user_route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteSaveRequestDto {
    
    private Long routeId;
    private LocalDate preferredStartDate;
    private LocalTime preferredStartTime;
}