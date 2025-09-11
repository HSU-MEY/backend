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
import java.util.Map;

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
    @Value("${tourapi.base.kor}")
    private String korBase;
    @Value("${tourapi.base.eng}")
    private String engBase;
    @Value("${tourapi.base.jpn}")
    private String jpnBase;
    @Value("${tourapi.base.chs}")
    private String chsBase;

    public List<RelatedResponseDto> fetchRelatedPlaces(double latitude, double longitude, String language) {

        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        String uriBase;
        String langCode;

        switch (language) {

            case "E":
                uriBase = engBase;
                langCode = "E";
                break;

            case "J":
                uriBase = jpnBase;
                langCode = "J";
                break;

            case "C":
                uriBase = chsBase;
                langCode = "C";
                break;

            default :
                uriBase = korBase;
                langCode = "K";
                break;
        }

        URI uri = UriComponentsBuilder
                .fromUriString(uriBase)
                .path("/locationBasedList2")
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", mobileOs)
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("mapX", longitude)   // 경도
                .queryParam("mapY", latitude)    // 위도
                .queryParam("radius", 500)       // 500m 반경
                .queryParam("arrange", "E")      // 거리순 정렬
                .queryParam("numOfRows", 10)
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

            List<RelatedResponseDto> out = new ArrayList<>();
            JsonNode items = om.readTree(body).at("/response/body/items/item");

            if (items.isArray()) {
                log.info("📍 locationBasedList2 {}건 lat={}, lon={}", items.size(), latitude, longitude);

                for (JsonNode it : items) {
                    String address = it.path("addr1").asText("");
                    int typeId = it.path("contenttypeid").asInt(0);
                    String typeName = getName(typeId, langCode);

                    out.add(new RelatedResponseDto(
                            it.path("title").asText(null),         // 제목
                            address,                                                    // 주소
                            it.path("tel").asText(null),           // 전화번호
                            it.path("firstimage").asText(null),    // 대표 이미지
                            typeName,                                                   // 관광타입명
                            it.path("dist").asDouble(0.0)          // 거리
                    ));
                }
            } else {
                log.warn("⚠️ locationBasedList2 결과 없음 lat={}, lon={}", latitude, longitude);
            }

            return out;
        } catch (Exception e) {
            log.error("❌ locationBasedList2 호출 실패 lat={}, lon={}", latitude, longitude, e);
        }
        return List.of();
    }

    private static final Map<Integer, Map<String, String>> CONTENT_TYPE_MAP = Map.of(
            12, Map.of("K", "관광지", "E", "Tourist Attraction", "J", "観光地", "C", "旅游景点"),
            14, Map.of("K", "문화시설", "E", "Cultural Facility", "J", "文化施設", "C", "文化设施"),
            15, Map.of("K", "축제/공연/행사", "E", "Festival/Performance/Event", "J", "祭り/公演/イベント", "C", "节日/演出/活动"),
            25, Map.of("K", "여행코스", "E", "Travel Course", "J", "旅行コース", "C", "旅行路线"),
            28, Map.of("K", "레포츠", "E", "Leisure Sports", "J", "レジャースポーツ", "C", "休闲运动"),
            32, Map.of("K", "숙박", "E", "Accommodation", "J", "宿泊", "C", "住宿"),
            38, Map.of("K", "쇼핑", "E", "Shopping", "J", "ショッピング", "C", "购物"),
            39, Map.of("K", "음식점", "E", "Restaurant", "J", "飲食店", "C", "餐厅")
    );

    public static String getName(int typeId, String langCode) {
        return CONTENT_TYPE_MAP.getOrDefault(typeId, Map.of())
                .getOrDefault(langCode, "Unknown");
    }
}
