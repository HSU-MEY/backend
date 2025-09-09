package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRelationRefreshService {

    private final PlaceRepository placeRepository;
    private final PlaceTourApiClient tourApiClient;
    private final PlaceMultilangUpsertService upsertService;

    private static final int MAX_RELATED = 20;

    /** 전체 Place 페이징 순회 */
    public void refreshAllRelatedPlaces() {
        log.info("[RelationRefresh] 시작");
        int page = 0, size = 200;
        Page<Place> chunk;
        do {
            chunk = placeRepository.findAll(PageRequest.of(page++, size));
            log.info("[RelationRefresh] 페이지 {} size={} count={}", page, size, chunk.getNumberOfElements());
            chunk.forEach(this::refreshOnePlaceSafely);
        } while (chunk.hasNext());
        log.info("[RelationRefresh] 종료");
    }

    private void refreshOnePlaceSafely(Place place) {
        log.debug("[RelationRefresh] 시작 placeId={} name={}", place.getPlaceId(), place.getNameKo());
        try {
            refreshOnePlace(place);
            log.info("[RelationRefresh] 성공 placeId={} name={}", place.getPlaceId(), place.getNameKo());
        } catch (Exception e) {
            log.warn("[RelationRefresh] placeId={} 실패: {}", place.getPlaceId(), e.getMessage(), e);
        } finally {
            log.debug("[RelationRefresh] 종료 placeId={} name={}", place.getPlaceId(), place.getNameKo());
        }
    }

    // 1개 Place에 대해: nameKo → 연관 contentId → Place 업서트 → relatedByPlaces 교체
    @Transactional
    public void refreshOnePlace(Place place) {
        log.info("[RelationRefresh] placeId={} nameKo={} areaCd={} siGunGuCd={}",
                place.getPlaceId(), place.getNameKo(), place.getAreaCd(), place.getSiGunGuCd());

        List<Long> relatedIds = new ArrayList<>();

        // API 호출
        var relatedItems = tourApiClient.fetchRelatedPlaces(
                place.getNameKo(),
                "202508",
                place.getAreaCd(),
                place.getSiGunGuCd()
        );

        if (relatedItems == null || relatedItems.isEmpty()) {
            log.info("[RelationRefresh] placeId={} → 연관 결과 없음", place.getPlaceId());
        } else {
            log.info("[RelationRefresh] placeId={} → {}개 연관 아이템", place.getPlaceId(), relatedItems.size());
        }

        // 결과 처리
        if (relatedItems != null) {
            for (var it : relatedItems) {
                if (relatedIds.size() >= MAX_RELATED) break;
                String cid = it.getRlteTatsCd();

                log.debug("[RelationRefresh] placeId={} → 업서트 cid={}", place.getPlaceId(), cid);
                Place related = upsertService.upsertByContentId(cid);

                if (!Objects.equals(related.getPlaceId(), place.getPlaceId())) {
                    relatedIds.add(related.getPlaceId());
                }
            }
        }

        // 중복 제거 후 업데이트
        List<Long> distinct = relatedIds.stream().distinct().toList();
        place.setRelatedByPlaces(distinct);
        log.info("[RelationRefresh] placeId={} → relatedByPlaces {}개 저장", place.getPlaceId(), distinct.size());
    }
}