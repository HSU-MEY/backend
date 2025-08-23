package com.mey.backend.domain.user_route.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRouteStatus {
    ALL("ALL"),
    NOT_STARTED("NOT_STARTED"),
    ON_GOING("ON_GOING"),
    COMPLETED("COMPLETED");

    private final String routeStatus;
}
