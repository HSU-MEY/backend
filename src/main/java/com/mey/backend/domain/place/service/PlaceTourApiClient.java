package com.mey.backend.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.place.dto.DetailCommonItemDto;
import com.mey.backend.domain.place.dto.RelatedPlaceItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceTourApiClient {

    private final ObjectMapper om = new ObjectMapper();

    @Value("${tourapi.service-key}") private String serviceKey;
    @Value("${tourapi.mobile-os}")  private String mobileOs;
    @Value("${tourapi.mobile-app}") private String mobileApp;
    @Value("${tourapi.base.related}") private String relatedBase;
    @Value("${tourapi.base.kor}")     private String korBase;
    @Value("${tourapi.base.eng}")     private String engBase;
    @Value("${tourapi.base.jpn}")     private String jpnBase;
    @Value("${tourapi.base.chs}")     private String chsBase;

    private RestClient client(String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }


    private String safeHead(String body, int max) {
        if (body == null) return "null";
        String t = body.trim();
        return t.substring(0, Math.min(max, t.length()));
    }

    // 1) 연관관광지: TarRlteTarService1/searchKeyword1
    // 필수: baseYm(YYYYMM), areaCd, signguCd, keyword
    public List<RelatedPlaceItemDto> fetchRelatedPlaces(String keyword, String baseYm, String areaCd, String signguCd) {

        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        String encodedKeword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        URI uri = UriComponentsBuilder
                .fromUriString(relatedBase)
                .path("/searchKeyword1")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("baseYm", baseYm)
                .queryParam("areaCd", areaCd)
                .queryParam("signguCd", signguCd)
                .queryParam("keyword", encodedKeword)
                .build(true).toUri();

        log.info("▶ TourAPI(Related) 호출: {}", uri);

        String body = null;
        try {
            body = RestClient.builder().baseUrl("")
                    .build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            log.info("◀ TourAPI 응답(앞 300자): {}", safeHead(body, 300));
        } catch (Exception e) {
            log.error("❌ TourAPI 호출 실패 keyword={}, uri={}", keyword, uri, e);
            return List.of();
        }

        if (body == null || body.isBlank()) {
            log.error("❌ TourAPI 빈 응답 keyword={}", keyword);
            return List.of();
        }

        String trimmed = body.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            // XML 에러 응답 처리
            String resultMsg = null, authMsg = null;
            try {
                Matcher m1 = Pattern.compile("<resultMsg>(.*?)</resultMsg>").matcher(trimmed);
                if (m1.find()) resultMsg = m1.group(1);
                Matcher m2 = Pattern.compile("<returnAuthMsg>(.*?)</returnAuthMsg>").matcher(trimmed);
                if (m2.find()) authMsg = m2.group(1);
            } catch (Exception ignore) {}
            log.error("❌ TourAPI XML 에러 keyword={}, resultMsg={}, returnAuthMsg={}, head={}",
                    keyword, resultMsg, authMsg, safeHead(trimmed, 200));
            return List.of();
        }

        try {
            List<RelatedPlaceItemDto> out = new ArrayList<>();
            JsonNode items = om.readTree(trimmed).at("/response/body/items/item");
            if (items.isArray()) {
                for (JsonNode it : items) {
                    out.add(new RelatedPlaceItemDto(
                            it.path("rlteTatsCd").asText(null),
                            it.path("rlteTatsNm").asText(null)
                    ));
                }
            } else if (!items.isMissingNode()) {
                out.add(new RelatedPlaceItemDto(
                        items.path("rlteTatsCd").asText(null),
                        items.path("rlteTatsNm").asText(null)
                ));
            }
            log.info("✔ TourAPI 파싱 성공 keyword={}, 결과 {}건", keyword, out.size());
            return out;
        } catch (Exception e) {
            log.error("❌ TourAPI JSON 파싱 실패 keyword={}, bodyHead={}", keyword, safeHead(trimmed, 200), e);
            return List.of();
        }
    }

    // 2) 다국어 detailCommon2 (Kor/Eng/Jpn/ChsService2 하위)
    private DetailCommonItemDto fetchDetailCommon(String baseUrl, String contentId) {

        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/detailCommon2")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("defaultYN", "Y")
                .queryParam("firstImageYN", "Y")
                .queryParam("areacodeYN", "Y")
                .queryParam("addrinfoYN", "Y")
                .queryParam("mapinfoYN", "Y")
                .queryParam("overviewYN", "Y")
                .build(true)
                .toUri();

        log.info("▶ TourAPI(detailCommon2) 호출: {}", uri);

        String body = null;
        try {
            body = RestClient.builder().baseUrl("")
                    .build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            log.info("◀ TourAPI 응답(앞 300자): {}", safeHead(body, 300));
        } catch (Exception e) {
            log.error("❌ TourAPI 호출 실패 detailCommon2 contentId={}, uri={}", contentId, uri, e);
            return null;
        }

        if (body == null || body.isBlank()) {
            log.error("❌ TourAPI 빈 응답 detailCommon2 contentId={}", contentId);
            return null;
        }

        String trimmed = body.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            // XML 에러 응답 처리
            String resultMsg = null, authMsg = null;
            try {
                Matcher m1 = Pattern.compile("<resultMsg>(.*?)</resultMsg>").matcher(trimmed);
                if (m1.find()) resultMsg = m1.group(1);
                Matcher m2 = Pattern.compile("<returnAuthMsg>(.*?)</returnAuthMsg>").matcher(trimmed);
                if (m2.find()) authMsg = m2.group(1);
            } catch (Exception ignore) {}
            log.error("❌ TourAPI XML 에러 detailCommon2 contentId={}, resultMsg={}, returnAuthMsg={}, head={}",
                    contentId, resultMsg, authMsg, safeHead(trimmed, 200));
            return null;
        }

        try {
            JsonNode item = om.readTree(trimmed).at("/response/body/items/item");
            if (item == null || item.isMissingNode() || item.isNull()) {
                log.warn("⚠ TourAPI detailCommon2 contentId={} 결과 없음", contentId);
                return null;
            }

            DetailCommonItemDto dto = DetailCommonItemDto.builder()
                    .contentId(item.path("contentid").asText(null))
                    .title(item.path("title").asText(null))
                    .overview(item.path("overview").asText(null))
                    .addr(item.path("addr1").asText(null))
                    .mapx(item.path("mapx").asText(null))
                    .mapy(item.path("mapy").asText(null))
                    .areaCode(item.path("areacode").asText(null))
                    .siGunGuCode(item.path("siGungu").asText(null))
                    .image(item.path("firstimage").asText(null))
                    .build();

            log.info("✔ TourAPI 파싱 성공 detailCommon2 contentId={}", contentId);
            return dto;
        } catch (Exception e) {
            log.error("❌ TourAPI JSON 파싱 실패 detailCommon2 contentId={}, bodyHead={}",
                    contentId, safeHead(trimmed, 200), e);
            return null;
        }
    }

    public DetailCommonItemDto fetchKorDetailCommon(String contentId) { return fetchDetailCommon(korBase, contentId); }
    public DetailCommonItemDto fetchEngDetailCommon(String contentId) { return fetchDetailCommon(engBase, contentId); }
    public DetailCommonItemDto fetchJpnDetailCommon(String contentId) { return fetchDetailCommon(jpnBase, contentId); }
    public DetailCommonItemDto fetchChsDetailCommon(String contentId) { return fetchDetailCommon(chsBase, contentId); }
}
