package com.mey.backend.domain.userRoute.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    NOT_STARTED("NOT_STARTED"),
    ON_GOING("ON_GOING"),
    COMPLETED("COMPLETED");

    private final String routeStatus;
}
