package com.mey.backend.domain.route.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Theme {
    KDRAMA("K_DRAMA"),
    KPOP("K_POP"),
    KFOOD("K_FOOD"),
    KFASHION("K_FASHION");

    private final String routeTheme;
}
