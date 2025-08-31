package com.mey.backend.domain.route.service;

import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.repository.PlaceRepository;
import com.mey.backend.domain.route.dto.*;
import com.mey.backend.domain.route.entity.*;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
    private static final int MIN_POPULARITY_SCORE = 10;
    private static final int COST_DIVISOR = 1000;

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final RegionRepository regionRepository;
    private final TransitClient transitClient; // 실제 구현: TmapTransitClient 등
    private final PlaceRepository placeRepository;   // 이미 있다면 사용
    private final SequencePlanner sequencePlanner;   // GptSequencePlanner 구현체 주입

    @Transactional
    public RouteCreateResponseDto createRouteByAI(CreateItineraryRequestDto req) {
        // 1) 입력 검증
        if (req.getPlaces() == null || req.getPlaces().size() < 2) {
            throw new IllegalArgumentException("2개 이상의 장소가 필요합니다.");
        }

        // 2) 좌표 → Place 확보 (placeId 있으면 findById, 없으면 INTERNAL 생성)
        ensureOriginalIndex(req.getPlaces());
        List<Place> selected = new ArrayList<>();
        for (CoordinateDto c : req.getPlaces()) {
            Place p;
            if (c.getPlaceId() != null) {
                p = placeRepository.findById(c.getPlaceId())
                        .orElseThrow(() -> new PlaceException(ErrorStatus.PLACE_NOT_FOUND));
            } else {
                p = createInternalPlace(c); // 프론트 name/addressKo 저장, 나머지는 더미
            }
            selected.add(p);
        }

        // 3) 방문 순서 최적화 (입력 인덱스 기준)
        List<CoordinateDto> coordsForPlan = req.getPlaces(); // lat/lng + originalIndex 사용
        SequencePlanner.PlanResult plan;
        try {
            plan = sequencePlanner.plan(coordsForPlan);
        } catch (Exception e) {
            // 실패 시 입력 순서 그대로
            List<Integer> fallback = IntStream.range(0, coordsForPlan.size()).boxed().toList();
            plan = new SequencePlanner.PlanResult(fallback, 0);
        }
        List<Integer> orderIdx = plan.order(); // 프론트에 내려줄 값
        // placeId 순서로도 만들어 두기(옵션)
        List<Long> placeIds = selected.stream().map(Place::getPlaceId).toList();
        List<Long> orderedPlaceIds = orderIdx.stream().map(placeIds::get).toList();

        // 4) 합계(총시간/거리/요금) 계산
        Totals totals = computeTotals(selected, orderedPlaceIds);
        int totalSec   = totals.totalDurationSec();
        int totalMin   = Math.max(1, totalSec / 60);
        int totalMeter = totals.totalDistanceMeters();
        int totalFare  = totals.totalFare();

        // 5) Route 저장
        Region routeRegion = chooseRegionByMajority(selected);
        String titleKo = "AI 추천 루트";
        String titleEn = "AI Recommended Route";
        String descKo  = String.format("총 %d개 장소, 약 %d분, %,dm 이동, 예상 교통비 %,d원",
                selected.size(), totalMin, totalMeter, totalFare);
        String descEn  = String.format("Total %d places, ~%d min, %,d m travel, est. fare %,d KRW",
                selected.size(), totalMin, totalMeter, totalFare);
        String imageUrl = resolveRouteImage(selected);

        Route route = Route.builder()
                .region(routeRegion)
                .titleKo(titleKo)
                .titleEn(titleEn)
                .descriptionKo(descKo)
                .descriptionEn(descEn)
                .imageUrl(imageUrl)
                .totalDurationMinutes(totalMin)     // m → 분
                .totalDistance(totalMeter) // DB에는 m로 저장
                .totalCost(totalFare)
                .themes(Collections.emptyList())
                .routeType(RouteType.AI)
                .build();
        routeRepository.save(route);

        // 6) RoutePlace 저장 (GPT가 준 순서대로)
        int order = 1;
        for (Integer idx : orderIdx) {
            Place p = selected.get(idx);
            routePlaceRepository.save(RoutePlace.builder()
                    .route(route)
                    .place(p)
                    .visitOrder(order++)
                    .recommendDurationMinutes(60) // 기본 체류시간(요구 시 req에서 받도록 확장)
                    .build());
        }

        // 7) 응답: 순서 + 기본 메타
        return RouteCreateResponseDto.builder()
                .routeId(route.getId())
                .titleKo(route.getTitleKo())
                .titleEn(route.getTitleEn())
                .descriptionKo(route.getDescriptionKo())
                .descriptionEn(route.getDescriptionEn())
                .imageUrl(route.getImageUrl())
                .totalDurationMinutes(route.getTotalDurationMinutes())
                .totalDistance(route.getTotalDistance() / 1000.0) // km로 내려줌
                .totalCost(route.getTotalCost())
                .themes(route.getThemes())
                .routeType(route.getRouteType())
                .regionName(route.getRegion() != null ? route.getRegion().getNameKo() : null)
                .order(orderIdx)                          // 프론트 핵심: 방문 순서
                .orderedPlaceIds(orderedPlaceIds)        // (옵션)
                .build();
    }

    // 요청 배열에서의 원래 위치 확정
    // 프론트가 보낸 각 장소(CoordinateDto)에 originalIndex가 비어 있으면, 서버가 0..N-1로 채워 넣음
    private void ensureOriginalIndex(List<CoordinateDto> coords) {
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i).getOriginalIndex() == null) coords.get(i).setOriginalIndex(i);
        }
    }

    // INTERNAL Place 생성: 받은 name/addressKo는 저장, 나머지는 더미로 채움
    private Place createInternalPlace(CoordinateDto c) {
        String nameKo = nonBlank(c.getName()) ? c.getName() : "선택한 위치";
        String addrKo = nonBlank(c.getAddressKo()) ? c.getAddressKo() : "";

        return placeRepository.save(Place.builder()
                .nameKo(nameKo)
                .nameEn(nameKo)                 // 임시: 한/영 동일
                .descriptionKo("정보 준비 중")   // 더미
                .descriptionEn("TBD")
                .longitude(c.getLng())
                .latitude(c.getLat())
                .imageUrl("/static/images/place-default.png") // 더미 이미지
                .address(addrKo)
                .contactInfo("")                // 더미
                .websiteUrl("")                 // 더미
                .kakaoPlaceId("")               // 더미
                .tourApiPlaceId("")             // 더미
                .openingHours(Map.of())         // 빈 JSON (nullable=false 대응)
                .themes(List.of())              // 빈 JSON
                .tags("")                       // 더미
                .costInfo("")                   // 더미
                .build());
    }

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }

    // 기존 computeTotals는 List<Long> ordered를 받으니 placeId 순서로 호출
    private Totals computeTotals(List<Place> selected, List<Long> orderedPlaceIds) {
        Map<Long, Place> byId = selected.stream()
                .collect(Collectors.toMap(Place::getPlaceId, p -> p));
        int sec = 0, dist = 0, fare = 0;
        for (int i = 0; i < orderedPlaceIds.size() - 1; i++) {
            Place from = byId.get(orderedPlaceIds.get(i));
            Place to   = byId.get(orderedPlaceIds.get(i + 1));
            TransitSegmentDto r = transitClient.route(
                    from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
                    to.getLatitude().doubleValue(),   to.getLongitude().doubleValue(),
                    null);
            sec  += r.getDurationSeconds();
            dist += r.getDistanceMeters();
            fare += r.getFare();
        }
        return new Totals(sec, dist, fare);
    }

    private record Totals(int totalDurationSec, int totalDistanceMeters, int totalFare) {
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
                .popularityScore(calculatePopularityScore(route))
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

    private Integer calculatePopularityScore(Route route) {
        return Math.max(100 - (route.getTotalCost() / COST_DIVISOR), MIN_POPULARITY_SCORE);
    }

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
