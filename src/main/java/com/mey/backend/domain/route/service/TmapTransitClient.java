package com.mey.backend.domain.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.route.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmapTransitClient implements TransitClient {

    @Value("${tmap.transit.base-url}")
    private String baseUrl;

    @Value("${tmap.transit.app-key}")
    private String appKey;

    @Value("${tmap.transit.lang:0}")
    private int lang;

    @Value("${tmap.transit.count:1}")
    private int count;

    @Value("${tmap.transit.format:json}")
    private String format;

    private final ObjectMapper om = new ObjectMapper();
    private RestClient rest;

    @PostConstruct
    void init() {
        this.rest = RestClient.builder()
                .baseUrl(baseUrl) // POST 전체 URL
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("appKey", appKey) // Tmap 요구 헤더
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public TransitSegmentDto route(String fromName, double fromLat, double fromLng,
                                   String toName,   double toLat,   double toLng,
                                   LocalDateTime departureTime) {

        // Tmap은 startX=경도, startY=위도 (WGS84)
        var body = """
        {
          "startX": "%f",
          "startY": "%f",
          "endX": "%f",
          "endY": "%f",
          "count": %d,
          "lang": %d,
          "format": "%s"
        }
        """.formatted(fromLng, fromLat, toLng, toLat, count, lang, format);

        JsonNode root;
        try {
            root = rest.post().body(body).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            log.warn("[TMAP] 호출 실패: {}", e.getMessage());
            // 실패 시 도보 더미 한 구간으로 폴백
            return walkFallback(fromName, fromLat, fromLng, toName, toLat, toLng);
        }

        try {
            // metaData.plan.itineraries[0]
            JsonNode it = root.path("metaData").path("plan").path("itineraries").get(0);
            if (it == null || it.isMissingNode()) {
                log.warn("[TMAP] itineraries 비어있음: {}", root.toString());
                return walkFallback(fromName, fromLat, fromLng, toName, toLat, toLng);
            }

            int totalFare = it.path("fare").path("regular").path("totalFare").asInt(0);
            // legs 배열을 하나의 Segment로 요약 (현재 API 응답은 A→B 전체 여정을 legs로 쪼갬)
            JsonNode legs = it.path("legs");

            int sumDistance = 0;
            int sumDuration = 0;
            List<TransitStepDto> steps = new ArrayList<>();
            StringBuilder summary = new StringBuilder();

            for (JsonNode leg : legs) {
                String mode = leg.path("mode").asText("WALK");
                int sectionTimeSec = (int) Math.round(leg.path("sectionTime").asDouble(0));
                int legDistance = (int) Math.round(leg.path("distance").asDouble(0));

                sumDuration += sectionTimeSec;
                sumDistance += legDistance;

                TransitStepDto.TransitStepDtoBuilder step = TransitStepDto.builder()
                        .distanceMeters(legDistance)
                        .durationSeconds(sectionTimeSec);

                // 기본 안내문
                String instruction = switch (mode) {
                    case "BUS" -> "버스 이동";
                    case "SUBWAY" -> "지하철 이동";
                    case "TRAIN" -> "기차 이동";
                    case "EXPRESSBUS" -> "고속/시외버스 이동";
                    case "AIRPLANE" -> "항공 이동";
                    case "FERRY" -> "해운 이동";
                    default -> "도보 이동";
                };

                // 노선명/정류장 수/라인
                String lineName = leg.path("route").asText(null); // 예: "지선:1128"
                Integer numStops = null;
                if (leg.has("passStopList") && leg.path("passStopList").has("stations")) {
                    numStops = leg.path("passStopList").path("stations").size();
                }

                // 폴리라인: 도보는 steps[*].linestring, 대중교통은 passShape.linestring
                List<LatLngDto> polyline = new ArrayList<>();
                if ("WALK".equals(mode)) {
                    if (leg.has("steps")) {
                        for (JsonNode s : leg.path("steps")) {
                            String lines = s.path("linestring").asText("");
                            polyline.addAll(parseLinestring(lines)); // lon,lat 공백구분
                        }
                    }
                } else {
                    String lines = leg.path("passShape").path("linestring").asText("");
                    polyline.addAll(parseLinestring(lines));
                }

                // 시작/끝 이름(있으면)
                String startName = leg.path("start").path("name").asText(null);
                String endName   = leg.path("end").path("name").asText(null);
                if (lineName != null && !lineName.isBlank()) {
                    instruction = instruction + " (" + lineName + ")";
                }
                if (startName != null && endName != null) {
                    instruction = instruction + " · " + startName + " → " + endName;
                }

                step.mode(mapMode(mode))
                        .instruction(instruction)
                        .lineName(lineName)
                        .headsign(null) // Tmap 응답에 headsign 유사 필드가 별도 노출되지 않음
                        .numStops(numStops)
                        .polyline(polyline);

                steps.add(step.build());

                // 요약문 구성
                if (summary.length() > 0) summary.append(" → ");
                summary.append(modeToKorean(mode));
            }

            return TransitSegmentDto.builder()
                    .fromName(fromName).fromLat(fromLat).fromLng(fromLng)
                    .toName(toName).toLat(toLat).toLng(toLng)
                    .distanceMeters(sumDistance)
                    .durationSeconds(sumDuration)
                    .fare(totalFare)
                    .summary(summary.toString())
                    .steps(steps)
                    .build();

        } catch (Exception e) {
            log.warn("[TMAP] 응답 파싱 실패: {}", e.getMessage());
            return walkFallback(fromName, fromLat, fromLng, toName, toLat, toLng);
        }
    }

    private List<LatLngDto> parseLinestring(String lines) {
        // "127.02551,37.637882 127.02552,37.637897 ..." (lon,lat 공백 구분)  [oai_citation:6‡SKopenAPI](https://skopenapi.readme.io/reference/%EB%8C%80%EC%A4%91%EA%B5%90%ED%86%B5-%EC%83%98%ED%94%8C-%EC%98%88%EC%A0%9C)
        List<LatLngDto> list = new ArrayList<>();
        if (lines == null || lines.isBlank()) return list;
        String[] pairs = lines.trim().split("\\s+");
        for (String pair : pairs) {
            String[] ll = pair.split(",");
            if (ll.length == 2) {
                double lon = Double.parseDouble(ll[0]);
                double lat = Double.parseDouble(ll[1]);
                list.add(LatLngDto.builder().lat(lat).lng(lon).build());
            }
        }
        return list;
    }

    private TransitStepDto.Mode mapMode(String mode) {
        return switch (mode) {
            case "BUS" -> TransitStepDto.Mode.BUS;
            case "SUBWAY" -> TransitStepDto.Mode.SUBWAY;
            case "TRAIN" -> TransitStepDto.Mode.RAIL;
            case "EXPRESSBUS" -> TransitStepDto.Mode.BUS;
            case "AIRPLANE" -> TransitStepDto.Mode.TAXI; // 별도 타입 없어서 임시
            case "FERRY" -> TransitStepDto.Mode.RAIL;    // 임시 매핑
            default -> TransitStepDto.Mode.WALK;
        };
    }

    private String modeToKorean(String mode) {
        return switch (mode) {
            case "BUS" -> "버스";
            case "SUBWAY" -> "지하철";
            case "TRAIN" -> "기차";
            case "EXPRESSBUS" -> "고속/시외버스";
            case "AIRPLANE" -> "항공";
            case "FERRY" -> "해운";
            default -> "도보";
        };
    }

    // API 실패시 하버사인 기반 도보 폴백
    private TransitSegmentDto walkFallback(String fromName, double fromLat, double fromLng,
                                           String toName, double toLat, double toLng) {
        int dist = haversineMeters(fromLat, fromLng, toLat, toLng);
        return TransitSegmentDto.builder()
                .fromName(fromName).fromLat(fromLat).fromLng(fromLng)
                .toName(toName).toLat(toLat).toLng(toLng)
                .distanceMeters(dist)
                .durationSeconds((int)Math.round(dist / 1.2)) // 도보 1.2m/s
                .fare(0)
                .summary("도보")
                .steps(List.of(
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("도보 이동(폴백)")
                                .distanceMeters(dist)
                                .durationSeconds((int)Math.round(dist / 1.2))
                                .build()
                ))
                .build();
    }

    private static int haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(R * c);
    }
}