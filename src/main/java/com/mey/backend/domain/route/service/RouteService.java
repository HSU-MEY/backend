package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.*;
import com.mey.backend.domain.route.entity.*;
import com.mey.backend.domain.route.repository.ItineraryRepository;
import com.mey.backend.domain.route.repository.RoutePlaceRepository;
import com.mey.backend.domain.route.repository.RouteRepository;
import com.mey.backend.domain.region.entity.Region;
import com.mey.backend.domain.region.repository.RegionRepository;
import com.mey.backend.global.exception.RouteException;
import com.mey.backend.global.payload.status.ErrorStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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
        return Math.max(100 - (route.getCost() / COST_DIVISOR), MIN_POPULARITY_SCORE);
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
                .cost(request.getCost())
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
                .cost(savedRoute.getCost())
                .totalDurationMinutes(savedRoute.getTotalDurationMinutes())
                .totalDistance(savedRoute.getTotalDistance())
                .totalCost(savedRoute.getTotalCost())
                .themes(savedRoute.getThemes())
                .routeType(savedRoute.getRouteType())
                .regionName(savedRoute.getRegion() != null ? savedRoute.getRegion().getNameKo() : null)
                .build();
    }
}
