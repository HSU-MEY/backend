package com.mey.backend.domain.route.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderedStopDto {
    private int order;
    private CoordinateDto coord;
}
