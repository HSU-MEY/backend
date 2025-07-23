package com.mey.backend.domain.route.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Theme {
    KDRAMA("KDRAMA"),
    KPOP("KPOP"),
    KFOOD("KFOOD"),
    KFASHION("KFASHION");

    private final String routeTheme;
}
