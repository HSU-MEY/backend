package com.mey.backend.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.place.dto.RelatedResponseDto;
import com.mey.backend.domain.place.entity.Place;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceTourApiClient {

    private final ObjectMapper om = new ObjectMapper();

    @Value("${tourapi.service-key}")
    private String serviceKey;
    @Value("${tourapi.mobile-os}")
    private String mobileOs;
    @Value("${tourapi.mobile-app}")
    private String mobileApp;
    @Value("${tourapi.base.related}")
    private String relatedBase;
    @Value("${tourapi.base.kor}")
    private String korBase;
    @Value("${tourapi.base.eng}")
    private String engBase;
    @Value("${tourapi.base.jpn}")
    private String jpnBase;
    @Value("${tourapi.base.chs}")
    private String chsBase;


    // TourAPI locationBasedList2 호출해서 areaCode, sigunguCode 반환
    public String[] fetchRegionCodesByLocation(double latitude, double longitude) {
        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder
                .fromUriString(korBase)
                .path("/locationBasedList2")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("mapX", longitude)   // 경도
                .queryParam("mapY", latitude)    // 위도
                .queryParam("radius", 700)       // 1000m 반경
                .queryParam("numOfRows", 1)      // 한 건만 조회
                .queryParam("pageNo", 1)
                .build(true).toUri();

        try {
            String body = RestClient.builder().baseUrl("")
                    .build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode items = om.readTree(body).at("/response/body/items/item");

            JsonNode target = items.isArray() && items.size() > 0 ? items.get(0) : items;

            if (target != null && !target.isMissingNode()) {
                String areaCode = target.path("lDongRegnCd").asText(null);
                String sigunguCode = target.path("lDongSignguCd").asText(null);

                if (Integer.parseInt(areaCode) > 0) {
                    log.info("✅ 좌표→행정코드 변환 성공 lat={}, lon={}, area={}, sigungu={}",
                            latitude, longitude, areaCode, sigunguCode);
                    return new String[]{areaCode, sigunguCode};
                } else {
                    log.warn("⚠️ 지역코드 없음 lat={}, lon={}, raw={}", latitude, longitude, target.toPrettyString());
                }
            } else {
                log.warn("⚠️ TourAPI locationBasedList2 결과 없음 lat={}, lon={}", latitude, longitude);
            }
        } catch (Exception e) {
            log.error("❌ TourAPI locationBasedList2 호출 실패 lat={}, lon={}", latitude, longitude, e);
        }
        return null;
    }

    public List<RelatedResponseDto> fetchRelatedPlacesInfo(
            Place place, String baseYm, String areaCd, String sigunguCd) {

        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        String encodedKeyword = URLEncoder.encode(place.getNameKo(), StandardCharsets.UTF_8);
        String fullSigunguCd = areaCd+ sigunguCd;
        URI uri = UriComponentsBuilder
                .fromUriString(relatedBase)
                .path("/searchKeyword1")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("baseYm", baseYm)
                .queryParam("areaCd", areaCd)
                .queryParam("signguCd", fullSigunguCd)
                .queryParam("keyword", encodedKeyword)
                .build(true).toUri();

        try {
            String body = RestClient.builder().baseUrl("")
                    .build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.info("📡 TourAPI searchKeyword1 호출 placeId={}, uri={}", place.getNameKo(), uri);
            log.debug("📡 searchKeyword1 응답 bodyHead={}", safeHead(body, 200));

            List<RelatedResponseDto> out = new ArrayList<>();
            JsonNode items = om.readTree(body).at("/response/body/items/item");

            if (items.isArray()) {
                log.info("🔎 연관관광지 {}건 placeId={}", items.size(), place.getPlaceId());

                for (JsonNode it : items) {
                    String name = it.path("rlteTatsNm").asText(null);
                    String regnCd = it.path("rlteRegnCd").asText(null);
                    String signgu = it.path("rlteSignguCd").asText(null);

                    Double distance = null;
                    double[] coords = fetchCoordsByKeyword(name, regnCd, signgu);
                    if (coords != null) {
                        distance = haversine(
                                place.getLatitude(), place.getLongitude(),
                                coords[0], coords[1]
                        );
                        log.info("✅ 거리계산 성공 from={} → to={} distance={}m",
                                place.getNameKo(), name, distance.intValue());
                    } else {
                        log.warn("⚠️ 좌표조회 실패 keyword={} (encoded={}), areaCd={}, signguCd={}, 응답={}",
                                name, encodedKeyword, areaCd, sigunguCd, safeHead(body, 200));
                    }

                    out.add(new RelatedResponseDto(
                            name,
                            it.path("rlteRegnNm").asText(null),
                            it.path("rlteSignguNm").asText(null),
                            it.path("rlteCtgryLclsNm").asText(null),
                            it.path("rlteCtgryMclsNm").asText(null),
                            it.path("rlteCtgrySclsNm").asText(null),
                            distance
                    ));
                }
            } else {
                log.warn("⚠️ 연관관광지 없음 placeId={}, bodyHead={}",
                        place.getPlaceId(), safeHead(body, 200));
            }

            return out;

        } catch (Exception e) {
            log.error("❌ TourAPI fetchRelatedPlacesInfo 실패 placeId={}, uri={}", place.getPlaceId(), uri, e);
            return List.of();
        }
    }

    // 연관관광지명으로 TourAPI(KorService2/searchKeyword2)에서 좌표(mapx/mapy)를 조회
    // - 우선 areaCode+sigunguCode로 시도
    // - 실패 시 areaCode만
    // - 그래도 실패하면 keyword만
    // @return [위도(lat), 경도(lon)] or null
    private double[] fetchCoordsByKeyword(String keyword, String areaCd, String signguCd) {
        // 1차: area + sigungu
        double[] coords = trySearchKeyword2(keyword, areaCd, signguCd);
        if (coords != null) {
            log.info("✅ 좌표조회 성공 (정밀검색) keyword={}, lat={}, lon={}", keyword, coords[0], coords[1]);
            return coords;
        }

        // 2차: area만
        coords = trySearchKeyword2(keyword, areaCd, null);
        if (coords != null) {
            log.info("✅ 좌표조회 성공 (시도 검색) keyword={}, lat={}, lon={}", keyword, coords[0], coords[1]);
            return coords;
        }

        // 3차: keyword만
        coords = trySearchKeyword2(keyword, null, null);
        if (coords != null) {
            log.info("✅ 좌표조회 성공 (전국 검색) keyword={}, lat={}, lon={}", keyword, coords[0], coords[1]);
            return coords;
        }

        log.warn("❌ 좌표조회 실패 keyword={}", keyword);
        return null;
    }

    // 실제로 TourAPI /searchKeyword2 호출을 수행하는 헬퍼 메서드
    private double[] trySearchKeyword2(String keyword, String areaCd, String signguCd) {
        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(korBase)
                .path("/searchKeyword2")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("keyword", encodedKeyword);

        if (areaCd != null) builder.queryParam("areaCode", areaCd);
        if (signguCd != null) builder.queryParam("sigunguCode", signguCd);

        URI uri = builder.build(true).toUri();

        try {
            String body = RestClient.builder().baseUrl("")
                    .build()
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("📡 searchKeyword2 호출 keyword={}, uri={}, bodyHead={}", keyword, uri, safeHead(body, 200));

            JsonNode items = om.readTree(body).at("/response/body/items/item");

            if (items.isArray() && items.size() > 0) {
                JsonNode first = items.get(0);
                double lon = first.path("mapx").asDouble();
                double lat = first.path("mapy").asDouble();
                return new double[]{lat, lon};
            } else if (!items.isMissingNode()) {
                double lon = items.path("mapx").asDouble();
                double lat = items.path("mapy").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            log.error("❌ TourAPI searchKeyword2 호출 실패 keyword={}, uri={}", keyword, uri, e);
        }
        return null;
    }

    private String safeHead(String body, int max) {
        if (body == null) return "null";
        String t = body.trim();
        return t.substring(0, Math.min(max, t.length()));
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
