package com.mey.backend.domain.place.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelatedResponseDto {
    private String title;
    private String address;
    private String tel;
    private String firstImage;
    private String contentTypeName;
    private double distance;
}