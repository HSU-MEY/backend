package com.mey.backend.domain.route.service;

import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.route.dto.*;
import com.mey.backend.domain.route.entity.*;
import com.mey.backend.domain.route.repository.ItineraryRepository;
import com.mey.backend.domain.route.repository.RoutePlaceRepository;
import com.mey.backend.domain.route.repository.RouteRepository;
import com.mey.backend.domain.region.entity.Region;
import com.mey.backend.domain.region.repository.RegionRepository;
import com.mey.backend.global.exception.PlaceException;
import com.mey.backend.global.exception.RouteException;
import com.mey.backend.global.payload.status.ErrorStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
    private static final int MIN_POPULARITY_SCORE = 10;
    private static final int COST_DIVISOR = 1000;

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final RegionRepository regionRepository;
    private final SequencePlanner planner; // GptSequencePlanner 등 @Component 구현체가 주입됨
    private final ItineraryRepository itineraryRepository; // JPA 리포지토리
    private final TransitClient transitClient; // 실제 구현: TmapTransitClient 등
    private final PlaceRepository placeRepository;   // 이미 있다면 사용
    private final SequencePlanner sequencePlanner;   // GptSequencePlanner 구현체 주입


    public RouteCreateResponseDto createRouteByAI(CreateItineraryRequestDto req) {
        // 1) 입력 검증
        if (req.getPlaces() == null || req.getPlaces().size() < 2) {
            throw new IllegalArgumentException("2개 이상의 장소가 필요합니다.");
        }

        // 2) 좌표 → Place 매핑 (placeId가 이미 있다면 우선 사용)
        List<Place> selected = req.getPlaces().stream().map(c -> {
            if (c.getPlaceId() != null) {
                return placeRepository.findById(c.getPlaceId())
                        .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
            }
            return placeRepository.findNearest(c.getLat(), c.getLng())
                    .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
        }).toList();

        // 3) 방문 순서 최적화
        List<Long> placeIds = selected.stream().map(Place::getPlaceId).toList();

        // plan()에 넣을 좌표(입력 인덱스와 1:1 대응)
        List<CoordinateDto> coordsForPlan = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Place p = selected.get(i);
            coordsForPlan.add(CoordinateDto.builder()
                    .lat(p.getLatitude().doubleValue())
                    .lng(p.getLongitude().doubleValue())
                    .originalIndex(i)
                    .build());
        }

        List<Long> ordered;
        try {
            // PlanResult 받기
            SequencePlanner.PlanResult plan = sequencePlanner.plan(coordsForPlan);

            // PlanResult.order() -> 입력 인덱스 → placeId로 매핑
            List<Integer> orderIdx = plan.order();
            // 방어 로직(인덱스 범위 체크)
            for (Integer idx : orderIdx) {
                if (idx == null || idx < 0 || idx >= placeIds.size()) {
                    throw new IllegalStateException("Planner가 잘못된 인덱스를 반환했습니다: " + idx);
                }
            }
            ordered = orderIdx.stream().map(placeIds::get).toList();

        } catch (Exception e) {
            // 실패 시 휴리스틱 폴백
            ordered = fallbackNearestNeighbor(placeIds);
        }

        // 4) 전체 합계(총시간/총거리/총요금) 계산을 위해 모든쌍 또는 인접쌍 요약 호출
        Totals totals = computeTotals(selected, ordered);

        // 5) Route 저장 (RouteType.AI)
        // 합계 계산 결과 (이미 가지고 있는 totals)
        int totalSec = totals.totalDurationSec();
        int totalMin = Math.max(1, totalSec / 60);
        int totalMeter = totals.totalDistanceMeters();
        int totalFare = totals.totalFare();

        // 지역은 선택된 place들의 region 중 '가장 많이 등장하는 region'으로 설정 (없으면 null 허용)
        Region routeRegion = chooseRegionByMajority(selected); // 아래 helper 참고

        // 타이틀/설명 기본값
        String titleKo = "AI 추천 루트";
        String titleEn = "AI Recommended Route";

        String descKo = String.format("총 %d개 장소, 약 %d분, %,dm 이동, 예상 교통비 %,d원",
                selected.size(), totalMin, totalMeter, totalFare);
        String descEn = String.format("Total %d places, ~%d min, %,d m travel, est. fare %,d KRW",
                selected.size(), totalMin, totalMeter, totalFare);

        // 대표 이미지: 1번 장소 이미지가 있으면 사용, 없으면 기본 이미지로
        String imageUrl = resolveRouteImage(selected); // 아래 helper 참고

        // themes: JSON 필수 → 빈 배열이라도 넣기
        List<Theme> themes = Collections.emptyList();

        // 교통비 총합 등 실제 합계
        double totalCostValue = (double) totalFare;

        // 최종 빌드
        Route route = Route.builder()
                .region(routeRegion)
                .titleKo(titleKo)
                .titleEn(titleEn)
                .descriptionKo(descKo)
                .descriptionEn(descEn)
                .imageUrl(imageUrl)
                .totalDurationMinutes(totalMin)          // int, not null
                .totalDistance((double) totalMeter)      // double, not null (미터 단위로 저장)
                .totalCost(totalCostValue)               // double, not null
                .themes(themes)                          // JSON not null
                .routeType(RouteType.AI)                 // enum, not null
                .build();

        routeRepository.save(route);

        // 6) RoutePlace 저장
        Map<Long, Place> byId = selected.stream()
                .collect(Collectors.toMap(Place::getPlaceId, p -> p));
        int order = 1;
        for (Long pid : ordered) {
            routePlaceRepository.save(RoutePlace.builder()
                    .route(route)
                    .place(byId.get(pid))
                    .visitOrder(order++)
                    .recommendDurationMinutes(60)
                    .build());
        }

        // 7) 응답 DTO로 반환
        return RouteCreateResponseDto.builder()
                .routeId(route.getId())
                .titleKo(route.getTitleKo())
                .titleEn(route.getTitleEn())
                .descriptionKo(route.getDescriptionKo())
                .descriptionEn(route.getDescriptionEn())
                .imageUrl(route.getImageUrl())
                .totalDurationMinutes(route.getTotalDurationMinutes())
                .totalDistance(route.getTotalDistance() / 1000.0) // km
                .totalCost(route.getTotalCost())
                .themes(route.getThemes())
                .routeType(route.getRouteType())
                .regionName(route.getRegion() != null ? route.getRegion().getNameKo() : null)
                .build();
    }

//    @Transactional(readOnly = true)
//    public StartRouteResponseDto startRoute(Long routeId, StartRouteRequestDto req) {
//        Route route = routeRepository.findById(routeId)
//                .orElseThrow(() -> new RouteException(ErrorStatus.ROUTE_NOT_FOUND));
//
//        List<RoutePlace> rps = routePlaceRepository
//                .findAllByRoute_RouteIdOrderByVisitOrderAsc(routeId);
//        if (rps.isEmpty()) throw new RouteException(ErrorStatus.ROUTE_PLACE_EMPTY);
//
//        List<TransitSegmentDto> segments = new ArrayList<>();
//        int totalSec = 0, totalDist = 0, totalFare = 0;
//
//        // CURRENT → #1
//        Place first = rps.get(0).getPlace();
//        var s0 = transitClient.route(
//                req.getCurrentLat(), req.getCurrentLng(),
//                first.getLatitude().doubleValue(), first.getLongitude().doubleValue(),
//                req.getDepartureTime());
//        segments.add(TransitSegmentDto.builder()
//                .fromName("CURRENT").fromLat(req.getCurrentLat()).fromLng(req.getCurrentLng())
//                .toName(first.getNameKo()).toLat(first.getLatitude().doubleValue()).toLng(first.getLongitude().doubleValue())
//                .summary(s0.summary()).distanceMeters(s0.distanceMeters()).durationSeconds(s0.durationSeconds()).fare(s0.fare())
//                .steps(s0.steps())   // TransitStepDto 리스트
//                .build());
//        totalSec += s0.durationSeconds();
//        totalDist += s0.distanceMeters();
//        totalFare += s0.fare();
//
//        // #i → #i+1
//        for (int i = 0; i < rps.size() - 1; i++) {
//            Place from = rps.get(i).getPlace();
//            Place to = rps.get(i + 1).getPlace();
//            var s = transitClient.route(
//                    from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
//                    to.getLatitude().doubleValue(), to.getLongitude().doubleValue(),
//                    null);
//            segments.add(TransitSegmentDto.builder()
//                    .fromName(from.getNameKo()).fromLat(from.getLatitude().doubleValue()).fromLng(from.getLongitude().doubleValue())
//                    .toName(to.getNameKo()).toLat(to.getLatitude().doubleValue()).toLng(to.getLongitude().doubleValue())
//                    .summary(s.summary()).distanceMeters(s.distanceMeters()).durationSeconds(s.durationSeconds()).fare(s.fare())
//                    .steps(s.steps())
//                    .build());
//            totalSec += s.durationSeconds();
//            totalDist += s.distanceMeters();
//            totalFare += s.fare();
//        }
//
//        return StartRouteResponseDto.builder()
//                .routeId(routeId)
//                .totalDurationSeconds(totalSec)
//                .totalDistanceMeters(totalDist)
//                .totalFare(totalFare)
//                .currency("KRW")
//                .segments(segments)
//                .build();
//    }

    private List<Long> fallbackNearestNeighbor(List<Long> ids) {
        if (ids.size() <= 2) return ids;
        List<Long> tour = new ArrayList<>();
        Set<Long> remain = new HashSet<>(ids);
        Long cur = ids.get(0);
        tour.add(cur);
        remain.remove(cur);
        while (!remain.isEmpty()) {
            Long best = null;
            int bestSec = Integer.MAX_VALUE;
            for (Long p : remain) {
                int sec = approxDuration(cur, p); // 간단 근사 (직선거리 비례 등)
                if (sec < bestSec) {
                    bestSec = sec;
                    best = p;
                }
            }
            tour.add(best);
            remain.remove(best);
            cur = best;
        }
        return tour;
    }

    private int approxDuration(Long a, Long b) {
        // 아주 단순 근사(직선거리/보정). 정확도 필요하면 transitClient를 한 번 호출.
        return 900; // 15분 가정
    }

    private record Totals(int totalDurationSec, int totalDistanceMeters, int totalFare) {
    }

    private Totals computeTotals(List<Place> selected, List<Long> ordered) {
        Map<Long, Place> byId = selected.stream()
                .collect(Collectors.toMap(Place::getPlaceId, p -> p));
        int sec = 0, dist = 0, fare = 0;
        for (int i = 0; i < ordered.size() - 1; i++) {
            Place from = byId.get(ordered.get(i));
            Place to = byId.get(ordered.get(i + 1));
            TransitSegmentDto r = transitClient.route(
                    from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
                    to.getLatitude().doubleValue(), to.getLongitude().doubleValue(),
                    null);

            sec += r.getDurationSeconds();
            dist += r.getDistanceMeters();
            fare += r.getFare();
        }
        return new Totals(sec, dist, fare);
    }

    public StartRouteResponseDto startRoute2(Long routeId, StartRouteRequestDto req) {

        // TODO 실제 구현:
        // 1) routeId로 DB에서 순서가 정해진 장소 목록(예: itinerary stops)을 조회
        // 2) 현재 위치 → 1번 장소, i → i+1 각 구간에 대해 외부 길찾기 API 호출
        // 3) 외부 API 응답을 TransitSegmentDto/TransitStepDto로 매핑

        // 지금은 더미 데이터 반환:
        // 가정: 해당 루트는 총 3개 장소로 구성
        var now = (req.getDepartureTime() != null) ? req.getDepartureTime() : LocalDateTime.now();

        // 현재 위치 → 장소
        TransitSegmentDto seg01 = TransitSegmentDto.builder()
                .fromName("현재 위치")
                .fromLat(req.getCurrentLat())
                .fromLng(req.getCurrentLng())
                .toName("장소1 · 카페 무지")
                .toLat(37.5668)   // 더미
                .toLng(126.9785)  // 더미
                .distanceMeters(850)
                .durationSeconds(600)
                .fare(0)
                .summary("도보 8분")
                .steps(List.of(
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("카페 무지까지 850m 도보 이동")
                                .distanceMeters(850)
                                .durationSeconds(600)
                                .polyline(List.of(
                                        LatLngDto.builder().lat(req.getCurrentLat()).lng(req.getCurrentLng()).build(),
                                        LatLngDto.builder().lat(37.5668).lng(126.9785).build()
                                ))
                                .build()
                ))
                .build();

        // 장소1 → 장소2 (버스)
        TransitSegmentDto seg12 = TransitSegmentDto.builder()
                .fromName("장소1 · 카페 무지")
                .fromLat(37.5668)
                .fromLng(126.9785)
                .toName("장소2 · 한강공원")
                .toLat(37.5283)
                .toLng(126.9326)
                .distanceMeters(5400)
                .durationSeconds(1500)
                .fare(1250)
                .summary("도보 3분 → 버스 6정거장 → 도보 2분")
                .steps(List.of(
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("무지앞 정류장까지 200m 이동")
                                .distanceMeters(200)
                                .durationSeconds(180)
                                .polyline(List.of(
                                        LatLngDto.builder().lat(37.5668).lng(126.9785).build(),
                                        LatLngDto.builder().lat(37.5665).lng(126.9779).build()
                                ))
                                .build(),
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.BUS)
                                .instruction("버스 273 탑승 → 한강공원 입구 하차")
                                .lineName("273")
                                .headsign("여의도 방면")
                                .numStops(6)
                                .distanceMeters(5000)
                                .durationSeconds(1200)
                                .polyline(List.of(
                                        LatLngDto.builder().lat(37.5665).lng(126.9779).build(),
                                        LatLngDto.builder().lat(37.5400).lng(126.9600).build(),
                                        LatLngDto.builder().lat(37.5286).lng(126.9341).build()
                                ))
                                .build(),
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("하차 후 200m 이동")
                                .distanceMeters(200)
                                .durationSeconds(120)
                                .polyline(List.of(
                                        LatLngDto.builder().lat(37.5286).lng(126.9341).build(),
                                        LatLngDto.builder().lat(37.5283).lng(126.9326).build()
                                ))
                                .build()
                ))
                .build();

        // 장소2 → 장소3 (지하철)
        TransitSegmentDto seg23 = TransitSegmentDto.builder()
                .fromName("장소2 · 한강공원")
                .fromLat(37.5283)
                .fromLng(126.9326)
                .toName("장소3 · 경복궁")
                .toLat(37.5796)
                .toLng(126.9770)
                .distanceMeters(7400)
                .durationSeconds(2100)
                .fare(1450)
                .summary("도보 5분 → 지하철 7정거장 → 도보 4분")
                .steps(List.of(
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("여의나루역까지 350m 이동")
                                .distanceMeters(350)
                                .durationSeconds(300)
                                .build(),
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.SUBWAY)
                                .instruction("5호선 탑승 후 환승 → 3호선 경복궁역 하차")
                                .lineName("5호선/3호선")
                                .headsign("경복궁 방면")
                                .numStops(7)
                                .distanceMeters(6800)
                                .durationSeconds(1560)
                                .build(),
                        TransitStepDto.builder()
                                .mode(TransitStepDto.Mode.WALK)
                                .instruction("출구에서 경복궁까지 250m 이동")
                                .distanceMeters(250)
                                .durationSeconds(240)
                                .build()
                ))
                .build();

        int totalDist = (seg01.getDistanceMeters() + seg12.getDistanceMeters() + seg23.getDistanceMeters());
        int totalDur = (seg01.getDurationSeconds() + seg12.getDurationSeconds() + seg23.getDurationSeconds());
        int totalFare = (seg01.getFare() + seg12.getFare() + seg23.getFare());

        return StartRouteResponseDto.builder()
                .routeId(routeId)
                .totalDistanceMeters(totalDist)
                .totalDurationSeconds(totalDur)
                .totalFare(totalFare)
                .currency("KRW")
                .segments(List.of(seg01, seg12, seg23))
                .build();
    }

    public CreateItineraryResponseDto createItinerary2(CreateItineraryRequestDto req) {
        if (req == null || req.getPlaces() == null || req.getPlaces().size() < 2)
            throw new IllegalArgumentException("places는 최소 2개 이상이어야 합니다.");
        if (req.getPlaceCount() != req.getPlaces().size())
            throw new IllegalArgumentException("placeCount와 places 길이가 일치해야 합니다.");
        if (req.getPlaces().stream().anyMatch(p -> p.getLat() == null || p.getLng() == null))
            throw new IllegalArgumentException("모든 좌표에 lat/lng가 필요합니다.");

        IntStream.range(0, req.getPlaces().size()).forEach(i -> {
            var c = req.getPlaces().get(i);
            if (c.getOriginalIndex() == null) c.setOriginalIndex(i);
        });

        SequencePlanner.PlanResult plan = planner.plan(req.getPlaces());

        Itinerary it = Itinerary.builder()
                .totalDistanceMeters(plan.totalDistanceMeters())
                .createdAt(LocalDateTime.now())
                .build();

        for (int order = 0; order < plan.order().size(); order++) {
            int idx = plan.order().get(order);
            CoordinateDto c = req.getPlaces().get(idx);

            it.getStops().add(
                    ItineraryStop.builder()
                            .itinerary(it)
                            .orderIndex(order)
                            .originalIndex(c.getOriginalIndex())
                            .lat(c.getLat())
                            .lng(c.getLng())
                            .build()
            );
        }

        Itinerary saved = itineraryRepository.save(it);

        List<OrderedStopDto> orderedStops = saved.getStops().stream()
                .sorted(Comparator.comparingInt(ItineraryStop::getOrderIndex))
                .map(s -> OrderedStopDto.builder()
                        .order(s.getOrderIndex())
                        .coord(CoordinateDto.builder()
                                .lat(s.getLat())
                                .lng(s.getLng())
                                .originalIndex(s.getOriginalIndex())
                                .build())
                        .build())
                .toList();

        return CreateItineraryResponseDto.builder()
                .itineraryId(saved.getId())
                .orderedStops(orderedStops)
                .totalDistanceMeters(saved.getTotalDistanceMeters())
                .build();
    }

    public RouteRecommendListResponseDto getRecommendedRoutes(List<Theme> themes, String region, int limit, int offset) {
        String themeJson = null;
        if (themes != null && !themes.isEmpty()) {
            List<String> themeNames = themes.stream().map(Theme::name).toList();
            themeJson = "[" + themeNames.stream()
                    .map(name -> "\"" + name + "\"")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("") + "]";
        }
        List<Route> allRoutes = routeRepository.findByThemesAndRegion(themeJson, region);

        // 수동으로 페이지네이션 적용
        int totalCount = allRoutes.size();
        List<Route> paginatedRoutes = allRoutes.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        List<RouteRecommendResponseDto> routes = paginatedRoutes.stream()
                .map(this::convertToRouteRecommendDto)
                .toList();

        if (routes.isEmpty()) {
            routes = createMockRouteRecommendData();
            totalCount = routes.size();
        }

        return RouteRecommendListResponseDto.builder()
                .routes(routes)
                .totalCount(totalCount)
                .build();
    }

    private RouteRecommendResponseDto convertToRouteRecommendDto(Route route) {
        return RouteRecommendResponseDto.builder()
                .routeId(route.getId())
                .title(route.getTitleKo())
                .description(route.getDescriptionKo())
                .theme(route.getThemes().isEmpty() ? "" : route.getThemes().get(0).name())
                .totalDistanceKm(BigDecimal.valueOf(route.getTotalDistance()))
                .totalDurationMinutes(route.getTotalDurationMinutes())
                .estimatedCost((int) route.getTotalCost())
                .thumbnailUrl(route.getImageUrl())
                //.popularityScore(calculatePopularityScore(route))
                .availableTimes(getAvailableTimes())
                .build();
    }

    private List<RouteRecommendResponseDto> createMockRouteRecommendData() {
        return Arrays.asList(
                RouteRecommendResponseDto.builder()
                        .routeId(1L)
                        .title("서울 명동 쇼핑 투어")
                        .description("명동의 주요 쇼핑 명소를 둘러보는 루트")
                        .theme("쇼핑")
                        .totalDistanceKm(new BigDecimal("3.5"))
                        .totalDurationMinutes(240)
                        .estimatedCost(50000)
                        .thumbnailUrl("https://example.com/thumb1.jpg")
                        .popularityScore(85)
                        .availableTimes(getAvailableTimes())
                        .build(),
                RouteRecommendResponseDto.builder()
                        .routeId(2L)
                        .title("강남 카페 투어")
                        .description("강남의 유명 카페들을 체험하는 루트")
                        .theme("음식")
                        .totalDistanceKm(new BigDecimal("2.8"))
                        .totalDurationMinutes(180)
                        .estimatedCost(30000)
                        .thumbnailUrl("https://example.com/thumb2.jpg")
                        .popularityScore(78)
                        .availableTimes(getAvailableTimes())
                        .build()
        );
    }

//    private Integer calculatePopularityScore(Route route) {
//        return Math.max(100 - (route.getCost() / COST_DIVISOR), MIN_POPULARITY_SCORE);
//    }

    private List<LocalTime> getAvailableTimes() {
        return Arrays.asList(
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0)
        );
    }

    public RouteDetailResponseDto getRouteDetail(Long routeId, LocalDate date, LocalTime startTime) {
        Route route = findRouteById(routeId);
        List<RoutePlace> routePlaces = routePlaceRepository.findByRouteIdOrderByVisitOrder(routeId);

        List<RouteDetailResponseDto.RoutePlaceDto> placeDtos = routePlaces.isEmpty()
                ? createMockRoutePlaces(startTime)
                : convertToRoutePlaceDtos(routePlaces, startTime);

        return buildRouteDetailResponse(route, placeDtos);
    }

    private Route findRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteException(ErrorStatus.ROUTE_NOT_FOUND));
    }

    private RouteDetailResponseDto buildRouteDetailResponse(Route route, List<RouteDetailResponseDto.RoutePlaceDto> placeDtos) {
        return RouteDetailResponseDto.builder()
                .routeId(route.getId())
                .title(route.getTitleKo())
                .description(route.getDescriptionKo())
                .theme(route.getThemes().isEmpty() ? "" : route.getThemes().get(0).name())
                .totalDistanceKm(BigDecimal.valueOf(route.getTotalDistance()))
                .totalDurationMinutes(route.getTotalDurationMinutes())
                .estimatedCost((int) route.getTotalCost())
                .suggestedStartTimes(getAvailableTimes())
                .routePlaces(placeDtos)
                .build();
    }

    private List<RouteDetailResponseDto.RoutePlaceDto> convertToRoutePlaceDtos(List<RoutePlace> routePlaces,
                                                                               LocalTime startTime) {
        LocalTime currentTime = Optional.ofNullable(startTime).orElse(DEFAULT_START_TIME);

        return routePlaces.stream()
                .map(routePlace -> convertToRoutePlaceDto(routePlace, currentTime))
                .toList();
    }

    private RouteDetailResponseDto.RoutePlaceDto convertToRoutePlaceDto(RoutePlace routePlace, LocalTime currentTime) {
        LocalTime arrivalTime = Optional.ofNullable(routePlace.getArrivalTime()).orElse(currentTime);
        LocalTime departureTime = Optional.ofNullable(routePlace.getDepartureTime())
                .orElse(arrivalTime.plusMinutes(routePlace.getRecommendDurationMinutes()));

        RouteDetailResponseDto.PlaceDto placeDto = buildPlaceDto(routePlace);

        return RouteDetailResponseDto.RoutePlaceDto.builder()
                .sequenceOrder(routePlace.getVisitOrder())
                .place(placeDto)
                .recommendedDurationMinutes(routePlace.getRecommendDurationMinutes())
                .estimatedArrivalTime(arrivalTime)
                .estimatedDepartureTime(departureTime)
                .notes(routePlace.getNotes())
                .build();
    }

    private RouteDetailResponseDto.PlaceDto buildPlaceDto(RoutePlace routePlace) {
        return RouteDetailResponseDto.PlaceDto.builder()
                .placeId(routePlace.getPlace().getPlaceId())
                .name(routePlace.getPlace().getNameKo())
                .description(routePlace.getPlace().getDescriptionKo())
                .latitude(BigDecimal.valueOf(routePlace.getPlace().getLatitude()))
                .longitude(BigDecimal.valueOf(routePlace.getPlace().getLongitude()))
                .address(routePlace.getPlace().getAddress())
                .imageUrls(Arrays.asList(routePlace.getPlace().getImageUrl()))
                .openingHours(routePlace.getPlace().getOpeningHours())
                .build();
    }

    private List<RouteDetailResponseDto.RoutePlaceDto> createMockRoutePlaces(LocalTime startTime) {
        LocalTime baseStartTime = Optional.ofNullable(startTime).orElse(DEFAULT_START_TIME);

        return Arrays.asList(
                RouteDetailResponseDto.RoutePlaceDto.builder()
                        .sequenceOrder(1)
                        .place(RouteDetailResponseDto.PlaceDto.builder()
                                .placeId(1L)
                                .name("명동 쇼핑센터")
                                .description("명동의 대표적인 쇼핑센터")
                                .latitude(new BigDecimal("37.563600"))
                                .longitude(new BigDecimal("126.982400"))
                                .address("서울특별시 중구 명동길 14")
                                .imageUrls(List.of("https://example.com/place1.jpg"))
                                .openingHours(Map.of(
                                        "monday", "10:00-22:00",
                                        "tuesday", "10:00-22:00",
                                        "wednesday", "10:00-22:00",
                                        "thursday", "10:00-22:00",
                                        "friday", "10:00-22:00",
                                        "saturday", "10:00-22:00",
                                        "sunday", "10:00-21:00"
                                ))
                                .build())
                        .recommendedDurationMinutes(90)
                        .estimatedArrivalTime(baseStartTime)
                        .estimatedDepartureTime(baseStartTime.plusMinutes(90))
                        .notes("쇼핑과 브런치를 즐길 수 있는 곳")
                        .build(),
                RouteDetailResponseDto.RoutePlaceDto.builder()
                        .sequenceOrder(2)
                        .place(RouteDetailResponseDto.PlaceDto.builder()
                                .placeId(2L)
                                .name("명동성당")
                                .description("역사적인 명동성당")
                                .latitude(new BigDecimal("37.563500"))
                                .longitude(new BigDecimal("126.986200"))
                                .address("서울특별시 중구 명동길 74")
                                .imageUrls(List.of("https://example.com/place2.jpg"))
                                .openingHours(Map.of(
                                        "monday", "06:00-21:00",
                                        "tuesday", "06:00-21:00",
                                        "wednesday", "06:00-21:00",
                                        "thursday", "06:00-21:00",
                                        "friday", "06:00-21:00",
                                        "saturday", "06:00-21:00",
                                        "sunday", "06:00-21:00"
                                ))
                                .build())
                        .recommendedDurationMinutes(60)
                        .estimatedArrivalTime(baseStartTime.plusMinutes(105))
                        .estimatedDepartureTime(baseStartTime.plusMinutes(165))
                        .notes("역사와 문화를 체험할 수 있는 장소")
                        .build()
        );
    }


    private Region chooseRegionByMajority(List<Place> places) {
        // place.getRegion()이 null일 수 있으니 방어
        return places.stream()
                .map(Place::getRegion)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null); // 전부 null이면 null 허용 (DB 제약 확인)
    }

    private String resolveRouteImage(List<Place> places) {
        // 1) 첫 장소의 이미지, 2) 없으면 기본 이미지
        for (Place p : places) {
            if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
                return p.getImageUrl();
            }
        }
        return "/static/images/route-default.png"; // 프로젝트 맞게 경로/URL 지정
    }

    public RouteCreateResponseDto createRoute(RouteCreateRequestDto request) {
        Region region = null;
        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new RouteException(ErrorStatus.ROUTE_NOT_FOUND));
        }

        Route route = Route.builder()
                .region(region)
                .titleKo(request.getTitleKo())
                .titleEn(request.getTitleEn())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .imageUrl(request.getImageUrl())
                .totalDurationMinutes(request.getTotalDurationMinutes())
                .totalDistance(request.getTotalDistance())
                .totalCost(request.getTotalCost())
                .themes(request.getThemes())
                .routeType(request.getRouteType())
                .build();

        Route savedRoute = routeRepository.save(route);

        return RouteCreateResponseDto.builder()
                .routeId(savedRoute.getId())
                .titleKo(savedRoute.getTitleKo())
                .titleEn(savedRoute.getTitleEn())
                .descriptionKo(savedRoute.getDescriptionKo())
                .descriptionEn(savedRoute.getDescriptionEn())
                .imageUrl(savedRoute.getImageUrl())
                .totalDurationMinutes(savedRoute.getTotalDurationMinutes())
                .totalDistance(savedRoute.getTotalDistance())
                .totalCost(savedRoute.getTotalCost())
                .themes(savedRoute.getThemes())
                .routeType(savedRoute.getRouteType())
                .regionName(savedRoute.getRegion() != null ? savedRoute.getRegion().getNameKo() : null)
                .build();
    }
}
