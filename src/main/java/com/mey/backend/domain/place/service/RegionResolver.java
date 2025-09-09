package com.mey.backend.domain.place.service;

import com.mey.backend.domain.place.dto.DetailCommonItemDto;
import com.mey.backend.domain.region.entity.Region;
import com.mey.backend.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RegionResolver {

    private final RegionRepository regionRepository;

    // areaCode -> Region.nameKo 매핑 테이블
    private static final Map<Integer, String> AREA_CODE_TO_REGION = Map.ofEntries(
            Map.entry(11, "서울"),
            Map.entry(26, "부산"),
            Map.entry(27, "대구"),
            Map.entry(28, "인천"),
            Map.entry(29, "광주"),
            Map.entry(30, "대전"),
            Map.entry(31, "울산"),
            Map.entry(36, "세종"),
            Map.entry(41, "경기"),
            Map.entry(43, "충북"),
            Map.entry(44, "충남"),
            Map.entry(46, "전남"),
            Map.entry(47, "경북"),
            Map.entry(48, "경남"),
            Map.entry(50, "제주"),
            Map.entry(51, "강원"),
            Map.entry(52, "전북")
    );

    public Region resolve(DetailCommonItemDto kor) {
        try {
            int areaCode = Integer.parseInt(kor.getAreaCode());
            String regionName = AREA_CODE_TO_REGION.get(areaCode);

            if (regionName == null) {
                throw new IllegalStateException("Unknown areaCode=" + areaCode);
            }

            return regionRepository.findByNameKo(regionName)
                    .orElseThrow(() -> new IllegalStateException("Region not found in DB: " + regionName));
        } catch (Exception e) {
            throw new IllegalStateException("RegionResolver failed for contentId=" + kor.getContentId(), e);
        }
    }
}