package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.DetailCommonItemDto;
import com.mey.backend.domain.place.entity.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlaceFactory {

   private final RegionResolver regionResolver;

    public Place createFromMultilang(
            DetailCommonItemDto kor,
            DetailCommonItemDto eng,
            DetailCommonItemDto jpn,
            DetailCommonItemDto chs) {

        String nameKo = nonBlank(kor != null ? kor.getTitle() : null, "미정");
        String nameEn = nonBlank(eng != null ? eng.getTitle() : null, nameKo);
        String nameJp = nonBlank(jpn != null ? jpn.getTitle() : null, nameKo);
        String nameCh = nonBlank(chs != null ? chs.getTitle() : null, nameKo);

        String descKo = nonBlank(kor != null ? kor.getOverview() : null, "");
        String descEn = nonBlank(eng != null ? eng.getOverview() : null, descKo);
        String descJp = nonBlank(jpn != null ? jpn.getOverview() : null, descKo);
        String descCh = nonBlank(chs != null ? chs.getOverview() : null, descKo);

        return Place.builder()
                .region(regionResolver.resolve(kor))
                .nameKo(nameKo)
                .nameEn(nameEn)
                .nameJp(nameJp)
                .nameCh(nameCh)
                .descriptionKo(descKo)
                .descriptionEn(descEn)
                .descriptionJp(descJp)
                .descriptionCh(descCh)
                .longitude(parseDoubleOr(kor != null ? kor.getMapx() : null, 0))
                .latitude(parseDoubleOr(kor != null ? kor.getMapy() : null, 0))
                .imageUrl(nonBlank(kor != null ? kor.getImage() : null, "https://…/placeholder.png"))
                .addressKo(nonBlank(kor != null ? kor.getAddr() : null, ""))
                .addressEn(nonBlank(eng != null ? eng.getAddr() : null, ""))
                .addressJp(nonBlank(jpn != null ? jpn.getAddr() : null, ""))
                .addressJp(nonBlank(chs != null ? chs.getAddr() : null, ""))
                .tourApiPlaceId(kor != null ? kor.getContentId() : null)
                .openingHours(Map.of())
                .themes(List.of()) // 어떻게 채울지 고민해보자
                .areaCd(kor.getAreaCode())
                .siGunGuCd(kor.getSiGunGuCode())
                .relatedByPlaces(List.of())
                .costInfo("정보없음")
                .build();
    }

    private String nonBlank(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private double parseDoubleOr(String v, double fallback) {
        try {
            return v == null ? fallback : Double.parseDouble(v);
        } catch (Exception e) {
            return fallback;
        }
    }
}