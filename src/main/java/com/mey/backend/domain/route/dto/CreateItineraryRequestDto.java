package com.mey.backend.domain.route.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateItineraryRequestDto {

    @Min(2)
    private int placeCount;

    @NotNull
    @NotEmpty
    private List<CoordinateDto> places; // 길이 == placeCount 여야 함
}
