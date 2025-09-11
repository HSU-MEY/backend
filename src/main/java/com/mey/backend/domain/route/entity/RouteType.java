package com.mey.backend.domain.route.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RouteType {
    NORMAL("NORMAL"),
    AI("AI"),               // AI가 생성한 루트
    POPULAR("POPULAR");     // MD가 선정한 루트

    private final String typeOfRoute;
}
