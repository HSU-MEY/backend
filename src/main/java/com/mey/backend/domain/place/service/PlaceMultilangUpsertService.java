package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.DetailCommonItemDto;
import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceMultilangUpsertService {

    private final PlaceRepository placeRepository;
    private final PlaceTourApiClient tourApiClient; // Kor/Eng/Jpn/Chs 각각 detailCommon2 호출 기능
    private final PlaceFactory placeFactory;        // 다국어 DTO -> Place 엔티티 변환

    @Transactional
    public Place upsertByContentId(String contentId) {
        log.info("[upsertByContentId] 요청 contentId={}", contentId);

        return placeRepository.findByTourApiPlaceId(contentId)
                .map(place -> {
                    log.info("[upsertByContentId] DB에서 기존 Place 발견 contentId={}", contentId);
                    return place;
                })
                .orElseGet(() -> {
                    log.info("[upsertByContentId] DB에 없음, 새 Place 생성 시작 contentId={}", contentId);
                    return createNewPlaceByContentId(contentId);
                });
    }

    private Place createNewPlaceByContentId(String contentId) {
        log.debug("[createNewPlaceByContentId] contentId={} - 한글 detailCommon2 호출", contentId);
        DetailCommonItemDto kor = tourApiClient.fetchKorDetailCommon(contentId);
        log.debug("[createNewPlaceByContentId] contentId={} - fetchKorDetailCommon 호출 완료", contentId);
        if (kor == null) {
            log.error("[createNewPlaceByContentId] detailCommon2(KOR) 결과 없음 contentId={}", contentId);
            throw new IllegalStateException("detailCommon2(KOR) not found for contentId=" + contentId);
        }
        log.info("[createNewPlaceByContentId] 한글 정보 로드 완료 contentId={} name={}", contentId, kor.getTitle());

        DetailCommonItemDto eng = tourApiClient.fetchEngDetailCommon(contentId);
        DetailCommonItemDto jpn = tourApiClient.fetchJpnDetailCommon(contentId);
        DetailCommonItemDto chs = tourApiClient.fetchChsDetailCommon(contentId);
        log.debug("[createNewPlaceByContentId] 다국어 정보 로드 완료 contentId={} eng?={} jpn?={} chs?={}",
                contentId, eng != null, jpn != null, chs != null);

        Place newPlace = placeFactory.createFromMultilang(kor, eng, jpn, chs);
        log.info("[createNewPlaceByContentId] Place 엔티티 변환 완료 contentId={} nameKo={}",
                contentId, newPlace.getNameKo());

        Place saved = placeRepository.save(newPlace);
        log.info("[createNewPlaceByContentId] Place 저장 완료 placeId={} contentId={}",
                saved.getPlaceId(), contentId);

        return saved;
    }
}