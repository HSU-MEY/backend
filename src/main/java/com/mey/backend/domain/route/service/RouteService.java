package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.RouteDetailResponseDto;
import com.mey.backend.domain.route.dto.RouteRecommendListResponseDto;
import com.mey.backend.domain.route.dto.RouteRecommendResponseDto;
import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.RoutePlace;
import com.mey.backend.domain.route.entity.Theme;
import com.mey.backend.domain.route.repository.RoutePlaceRepository;
import com.mey.backend.domain.route.repository.RouteRepository;
import com.mey.backend.global.exception.RouteException;
import com.mey.backend.global.payload.status.ErrorStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
    private static final int MIN_POPULARITY_SCORE = 10;
    private static final int COST_DIVISOR = 1000;

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;

    public RouteRecommendListResponseDto getRecommendedRoutes(Theme theme, String region, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Route> routePage = routeRepository.findByThemeAndRegion(theme, region, pageable);

        List<RouteRecommendResponseDto> routes = routePage.getContent().stream()
                .map(this::convertToRouteRecommendDto)
                .toList();

        if (routes.isEmpty()) {
            routes = createMockRouteRecommendData();
        }

        return RouteRecommendListResponseDto.builder()
                .routes(routes)
                .totalCount((int) routePage.getTotalElements())
                .build();
    }


    private RouteRecommendResponseDto convertToRouteRecommendDto(Route route) {
        return RouteRecommendResponseDto.builder()
                .routeId(route.getId())
                .title(route.getTitleKo())
                .description(route.getDescriptionKo())
                .theme(route.getTheme().name())
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
                .theme(route.getTheme().name())
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
                                .imageUrls(Arrays.asList("https://example.com/place1.jpg"))
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
                                .imageUrls(Arrays.asList("https://example.com/place2.jpg"))
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

}
