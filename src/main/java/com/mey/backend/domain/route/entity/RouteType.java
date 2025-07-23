package com.mey.backend.domain.route.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RouteType {
    AI("AI"),
    POPULAR("POPULAR"),
    THEMED("THEMED");

    private final String typeOfRoute;
}
