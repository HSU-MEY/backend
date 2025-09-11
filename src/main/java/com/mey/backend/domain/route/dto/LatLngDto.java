package com.mey.backend.domain.route.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatLngDto {
    private Double lat;
    private Double lng;
}
