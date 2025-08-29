package com.mey.backend.domain.route.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartRouteRequestDto {

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double currentLat;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double currentLng;

    // 옵션: 출발 시각(없으면 서버 현재 시각 사용 가능)
    private LocalDateTime departureTime;
}
