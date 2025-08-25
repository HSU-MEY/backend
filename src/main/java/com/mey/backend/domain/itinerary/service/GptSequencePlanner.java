//package com.mey.backend.domain.itinerary.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mey.backend.domain.itinerary.dto.CoordinateDto;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//import java.util.stream.IntStream;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class GptSequencePlanner implements SequencePlanner {
//
//    @Value("${openai.api.key}")
//    private String apiKey;
//
//    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
//    private String apiUrl;
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public PlanResult plan(List<CoordinateDto> points) {
//        try {
//            String prompt = buildPrompt(points);
//            String gptResponse = callGpt(prompt);
//            return parseGptResponse(gptResponse, points.size());
//        } catch (Exception e) {
//            log.warn("GPT API 호출 실패, 기본 순서 반환: {}", e.getMessage());
//            return fallbackPlan(points);
//        }
//    }
//
//    private String buildPrompt(List<CoordinateDto> points) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("다음 좌표들을 가장 효율적으로 방문하는 순서를 찾아주세요.\n");
//        sb.append("총 거리가 최소가 되도록 TSP(외판원 문제) 알고리즘을 적용해주세요.\n\n");
//
//        for (int i = 0; i < points.size(); i++) {
//            CoordinateDto p = points.get(i);
//            sb.append(String.format("지점%d: 위도 %.6f, 경도 %.6f", i, p.getLat(), p.getLng()));
//            if (p.getName() != null) sb.append(" (" + p.getName() + ")");
//            sb.append("\n");
//        }
//
//        sb.append("\n응답 형식:\n");
//        sb.append("ORDER: [0,2,1,3] (방문순서 인덱스)\n");
//        sb.append("DISTANCE: 15420 (총 거리 미터)\n");
//        sb.append("DURATION: 1800 (총 시간 초)\n");
//        sb.append("SUMMARY: 총 15.4km, 약 30분 소요\n");
//
//        return sb.toString();
//    }
//
//    private String callGpt(String prompt) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + apiKey);
//        headers.set("Content-Type", "application/json");
//
//        Map<String, Object> requestBody = Map.of(
//                "model", "gpt-3.5-turbo",
//                "messages", List.of(
//                        Map.of("role", "user", "content", prompt)
//                ),
//                "max_tokens", 500,
//                "temperature", 0.3
//        );
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        GptResponse response = restTemplate.exchange(
//                apiUrl, HttpMethod.POST, entity, GptResponse.class
//        ).getBody();
//
//        return response.choices().get(0).message().content();
//    }
//
//    private PlanResult parseGptResponse(String response, int pointCount) {
//        try {
//            List<Integer> order = parseOrder(response, pointCount);
//            Integer distance = parseValue(response, "DISTANCE:");
//            Integer duration = parseValue(response, "DURATION:");
//            String summary = parseString(response, "SUMMARY:");
//
//            return new PlanResult(order, distance, duration, null, summary);
//        } catch (Exception e) {
//            log.warn("GPT 응답 파싱 실패: {}", e.getMessage());
//            return fallbackPlan(pointCount);
//        }
//    }
//
//    private List<Integer> parseOrder(String response, int pointCount) {
//        Pattern pattern = Pattern.compile("ORDER:\\s*\\[(\\d+(?:,\\s*\\d+)*)\\]");
//        var matcher = pattern.matcher(response);
//
//        if (matcher.find()) {
//            String[] indices = matcher.group(1).split(",\\s*");
//            return List.of(indices).stream()
//                    .mapToInt(Integer::parseInt)
//                    .boxed()
//                    .toList();
//        }
//
//        // 파싱 실패시 기본 순서
//        return IntStream.range(0, pointCount).boxed().toList();
//    }
//
//    private Integer parseValue(String response, String key) {
//        Pattern pattern = Pattern.compile(key + "\\s*(\\d+)");
//        var matcher = pattern.matcher(response);
//        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
//    }
//
//    private String parseString(String response, String key) {
//        Pattern pattern = Pattern.compile(key + "\\s*(.+)");
//        var matcher = pattern.matcher(response);
//        return matcher.find() ? matcher.group(1).trim() : null;
//    }
//
//    private PlanResult fallbackPlan(List<CoordinateDto> points) {
//        return fallbackPlan(points.size());
//    }
//
//    private PlanResult fallbackPlan(int pointCount) {
//        List<Integer> defaultOrder = IntStream.range(0, pointCount).boxed().toList();
//        return new PlanResult(defaultOrder, null, null, null, "기본 순서로 설정됨");
//    }
//
//    // GPT API 응답 DTO
//    record GptResponse(List<Choice> choices) {}
//    record Choice(Message message) {}
//    record Message(String content) {}
//}