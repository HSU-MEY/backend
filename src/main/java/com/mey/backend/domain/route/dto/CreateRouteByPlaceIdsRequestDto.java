package com.mey.backend.domain.route.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRouteByPlaceIdsRequestDto {
    @NotEmpty
    private List<Long> placeIds;
}
