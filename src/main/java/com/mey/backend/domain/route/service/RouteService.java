package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.*;
import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.RoutePlace;
import com.mey.backend.domain.route.entity.Theme;
import com.mey.backend.domain.route.repository.RoutePlaceRepository;
import com.mey.backend.domain.route.repository.RouteRepository;
import com.mey.backend.domain.route.repository.RouteTransportRepository;
import com.mey.backend.global.exception.RouteException;
import com.mey.backend.global.payload.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final RouteTransportRepository routeTransportRepository;

    public RouteRecommendListResponseDto getRecommendedRoutes(String theme, String region, LocalDate date, int limit, int offset) {
        Theme themeEnum = null;
        if (theme != null && !theme.isEmpty()) {
            try {
                themeEnum = Theme.valueOf(theme.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 테마인 경우 무시
            }
        }

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Route> routePage = routeRepository.findByThemeAndRegion(themeEnum, region, pageable);

        List<RouteRecommendResponseDto> routes = routePage.getContent().stream()
            .map(this::convertToRouteRecommendDto)
            .collect(Collectors.toList());

        // DB에 데이터가 없는 경우 모킹 데이터 반환
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
        // 비용이 낮을수록 높은 점수 (간단한 로직)
        return Math.max(100 - (route.getCost() / 1000), 10);
    }

    private List<LocalTime> getAvailableTimes() {
        return Arrays.asList(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(14, 0)
        );
    }

    public RouteDetailResponseDto getRouteDetail(Long routeId, LocalDate date, LocalTime startTime) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new RouteException(ErrorStatus.ROUTE_NOT_FOUND));

        // 실제 RoutePlace 데이터 조회
//        List<RoutePlace> routePlaces = routePlaceRepository.findByRouteIdOrderByVisitOrder(routeId);
//        List<RouteTransport> routeTransports = routeTransportRepository.findByRouteId(routeId);
//
//        // 데이터가 있으면 실제 데이터 사용, 없으면 모킹 데이터 사용
//        List<RouteDetailResponseDto.RoutePlaceDto> placeDtos;
//        List<RouteDetailResponseDto.RouteTransportDto> transportDtos;
//
//        if (!routePlaces.isEmpty()) {
//            placeDtos = convertToRoutePlaceDtos(routePlaces, startTime);
//        } else {
//            placeDtos = createMockRoutePlaces(startTime);
//        }
//
//        if (!routeTransports.isEmpty()) {
//            transportDtos = convertToRouteTransportDtos(routeTransports);
//        } else {
//            transportDtos = createMockRouteTransports();
//        }

        return RouteDetailResponseDto.builder()
            .routeId(route.getId())
            .title(route.getTitleKo())
            .description(route.getDescriptionKo())
            .theme(route.getTheme().name())
            .totalDistanceKm(BigDecimal.valueOf(route.getTotalDistance()))
            .totalDurationMinutes(route.getTotalDurationMinutes())
            .estimatedCost((int) route.getTotalCost())
            .suggestedStartTimes(getAvailableTimes())
//            .routePlaces(placeDtos)
//            .routeTransports(transportDtos)
            .build();
    }

    private List<RouteDetailResponseDto.RoutePlaceDto> convertToRoutePlaceDtos(List<RoutePlace> routePlaces, LocalTime startTime) {
        LocalTime currentTime = startTime != null ? startTime : LocalTime.of(10, 0);
        
        return routePlaces.stream()
            .map(routePlace -> {
                LocalTime arrivalTime = routePlace.getArrivalTime() != null ? routePlace.getArrivalTime() : currentTime;
                LocalTime departureTime = routePlace.getDepartureTime() != null ? 
                    routePlace.getDepartureTime() : arrivalTime.plusMinutes(routePlace.getRecommendDurationMinutes());
                
                RouteDetailResponseDto.PlaceDto placeDto = RouteDetailResponseDto.PlaceDto.builder()
                    .placeId(routePlace.getPlace().getPlaceId())
                    .name(routePlace.getPlace().getNameKo())
                    .description(routePlace.getPlace().getDescriptionKo())
                    .latitude(BigDecimal.valueOf(routePlace.getPlace().getLatitude()))
                    .longitude(BigDecimal.valueOf(routePlace.getPlace().getLongitude()))
                    .address(routePlace.getPlace().getAddress())
                    .imageUrls(Arrays.asList(routePlace.getPlace().getImageUrl()))
                    .openingHours(routePlace.getPlace().getOpeningHours())
                    .build();
                
                return RouteDetailResponseDto.RoutePlaceDto.builder()
                    .sequenceOrder(routePlace.getVisitOrder())
                    .place(placeDto)
                    .recommendedDurationMinutes(routePlace.getRecommendDurationMinutes())
                    .estimatedArrivalTime(arrivalTime)
                    .estimatedDepartureTime(departureTime)
                    .notes(routePlace.getNotes())
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<RouteDetailResponseDto.RouteTransportDto> convertToRouteTransportDtos(List<RouteTransport> routeTransports) {
        return routeTransports.stream()
            .map(transport -> RouteDetailResponseDto.RouteTransportDto.builder()
                .fromPlaceName(transport.getFromPlace().getPlace().getNameKo())
                .toPlaceName(transport.getToPlace().getPlace().getNameKo())
                .transportMode(transport.getTransportMode())
                .durationMinutes(transport.getDurationMinutes())
                .distanceMeters(transport.getDistanceMeters())
                .costKrw(transport.getCostKrw())
                .directions(transport.getDirections())
                .build())
            .collect(Collectors.toList());
    }

    private List<RouteDetailResponseDto.RoutePlaceDto> createMockRoutePlaces(LocalTime startTime) {
        LocalTime baseStartTime = startTime != null ? startTime : LocalTime.of(10, 0);
        
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

    private List<RouteDetailResponseDto.RouteTransportDto> createMockRouteTransports() {
        return Arrays.asList(
            RouteDetailResponseDto.RouteTransportDto.builder()
                .fromPlaceName("명동 쇼핑센터")
                .toPlaceName("명동성당")
                .transportMode("도보")
                .durationMinutes(15)
                .distanceMeters(800)
                .costKrw(0)
                .directions(Map.of("description", "명동길을 따라 직진"))
                .build()
        );
    }
}
