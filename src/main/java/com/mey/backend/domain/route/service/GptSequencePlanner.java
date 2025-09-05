package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.CoordinateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class GptSequencePlanner implements SequencePlanner {

    @Value("${openai.api.key}")
    private String apiKey;

    // 기본값: Chat Completions 엔드포인트 (풀 URL)
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private RestClient rest;

    @PostConstruct
    void init() {
        this.rest = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PlanResult plan(List<CoordinateDto> points) {
        try {
            String prompt = buildPrompt(points);
            String gptResponse = callGpt(prompt);
            return parseGptResponse(gptResponse, points);
        } catch (Exception e) {
            log.warn("GPT API 호출 실패, 기본 순서 반환: {}", e.getMessage());
            return fallbackPlan(points);
        }
    }

    // GPT에게 '순서'만 요구
    private String buildPrompt(List<CoordinateDto> points) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
        다음 좌표들을 한 번씩 방문하는 최적의 순서를 구하세요.
        목적: 총 이동거리(직선거리, 하버사인 근사)가 최소가 되도록.

        반드시 아래 EXACT 포맷으로만 출력하세요. 다른 설명/문장/코드블록 금지.
        ORDER: [0, 2, 1, ...]

        입력 좌표 (인덱스: 위도, 경도):
        """);

        for (int i = 0; i < points.size(); i++) {
            CoordinateDto p = points.get(i);
            sb.append(String.format("%d: %.6f, %.6f%n", i, p.getLat(), p.getLng()));
        }
        return sb.toString();
    }

    private String callGpt(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 500,
                "temperature", 0.3
        );

        GptResponse resp = rest.post()
                .uri("") // baseUrl에 풀 URL 사용 중이므로 빈 경로
                .body(body)
                .retrieve()
                .body(GptResponse.class);

        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new IllegalStateException("Empty GPT response");
        }
        return resp.choices().get(0).message().content();
    }

    // 순서만 파싱 → 서버에서 하버사인으로 총 거리 계산
    private PlanResult parseGptResponse(String response, List<CoordinateDto> points) {
        try {
            List<Integer> order = parseOrder(response, points.size());
            int totalMeters = computeTotalMeters(points, order);
            return new PlanResult(order, totalMeters);
        } catch (Exception e) {
            log.warn("GPT 응답 파싱 실패: {}", e.getMessage());
            return fallbackPlan(points);
        }
    }

    private List<Integer> parseOrder(String response, int pointCount) {
        Pattern pattern = Pattern.compile("ORDER:\\s*\\[(\\d+(?:,\\s*\\d+)*)\\]");
        var matcher = pattern.matcher(response);

        if (matcher.find()) {
            String[] indices = matcher.group(1).split(",\\s*");
            return Arrays.stream(indices)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        }
        // 파싱 실패 시 기본 순서
        return IntStream.range(0, pointCount).boxed().toList();
    }

    // 하버사인 거리(미터) 계산
    private static int haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(R * c);
    }

    private int computeTotalMeters(List<CoordinateDto> points, List<Integer> order) {
        if (order == null || order.size() <= 1) return 0;
        int total = 0;
        for (int i = 0; i < order.size() - 1; i++) {
            CoordinateDto a = points.get(order.get(i));
            CoordinateDto b = points.get(order.get(i + 1));
            total += haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        }
        return total;
    }

    private PlanResult fallbackPlan(List<CoordinateDto> points) {
        List<Integer> defaultOrder = IntStream.range(0, points.size()).boxed().toList();
        int total = computeTotalMeters(points, defaultOrder);
        return new PlanResult(defaultOrder, total);
    }

    // GPT API 응답 DTO
    record GptResponse(List<Choice> choices) {}
    record Choice(Message message) {}
    record Message(String content) {}
}