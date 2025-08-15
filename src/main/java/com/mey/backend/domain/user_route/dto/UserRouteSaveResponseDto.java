package com.mey.backend.domain.user_route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteSaveResponseDto {
    
    private Long savedRouteId;
}
