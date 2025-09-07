package com.mey.backend.domain.route.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    @JsonValue
    public String getRouteTheme() {
        return routeTheme;
    }

    @JsonCreator
    public static Theme fromString(String value) {
        for (Theme theme : Theme.values()) {
            if (theme.routeTheme.equals(value) || theme.name().equals(value)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("Invalid theme value: " + value);
    }
}
