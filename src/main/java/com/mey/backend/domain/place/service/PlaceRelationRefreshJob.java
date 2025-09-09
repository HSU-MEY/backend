package com.mey.backend.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceRelationRefreshJob {

    private final PlaceRelationRefreshService refreshService;

    @Scheduled(cron = "*/30 * * * * *")
    public void runDaily() {

        log.info("PlaceRelationRefreshJob 실행 전");
        refreshService.refreshAllRelatedPlaces();
        log.info("PlaceRelationRefreshJob 실행 완료");
    }
}