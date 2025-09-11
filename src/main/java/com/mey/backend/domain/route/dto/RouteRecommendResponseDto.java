package com.mey.backend.domain.route.dto;

import com.mey.backend.domain.route.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendResponseDto {

    private Long id;              // 루트 ID
    private String imageUrl;      // 이미지 URL

    private String titleKo;       // 타이틀 (한글)
    private String titleEn;       // 타이틀 (영문)

    private String regionNameKo;  // 지역명 (한글)
    private String regionNameEn;  // 지역명 (영문)

    private String descriptionKo; // 설명 (한글)
    private String descriptionEn; // 설명 (영문)

    private List<Theme> themes;   // 테마 리스트
}
