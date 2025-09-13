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

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final RegionRepository regionRepository;
    private final TransitClient transitClient; // 실제 구현: TmapTransitClient 등
    private final PlaceRepository placeRepository;
    private final SequencePlanner sequencePlanner;   // GptSequencePlanner 구현체 주입

    @Transactional(readOnly = true)
    public StartRouteResponse startRoute(Long routeId, double latitude, double longitude) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 루트입니다."));

        List<RoutePlace> routePlaces = routePlaceRepository.findByRouteIdOrderByVisitOrder(routeId);

        List<Place> places = routePlaces.stream()
                .map(RoutePlace::getPlace)   // 중간 엔티티에서 Place 꺼내기
                .toList();
        if (places.isEmpty()) {
            throw new IllegalStateException("루트에 장소가 없습니다.");
        }

        List<TransitSegmentDto> segments = new ArrayList<>();

        // 현재 위치 → 첫 번째 장소
        Place firstPlace = places.get(0);
        segments.add(
                transitClient.route(
                        "현재 위치",
                        latitude, longitude,
                        firstPlace.getNameKo(),
                        firstPlace.getLatitude(), firstPlace.getLongitude(),
                        null
                )
        );

        // i → i+1
        for (int i = 0; i < places.size() - 1; i++) {
            Place from = places.get(i);
            Place to = places.get(i + 1);

            segments.add(
                    transitClient.route(
                            from.getNameKo(),
                            from.getLatitude(), from.getLongitude(),
                            to.getNameKo(),
                            to.getLatitude(), to.getLongitude(),
                            null
                    )
            );
        }

        return StartRouteResponse.builder()
                .segments(segments)
                .build();
    }

    @Transactional
    public RouteCreateResponseDto createRouteByAI(CreateRouteByPlaceIdsRequestDto req) {
        // 1) 검증
        if (req.getPlaceIds() == null || req.getPlaceIds().size() < 2) {
            throw new IllegalArgumentException("2개 이상의 placeId가 필요합니다.");
        }

        // 2) placeId → Place 조회 (요청 순서 보존)
        // findAllById는 순서를 보장하지 않으니, map으로 받아서 placeIds 순회
        Map<Long, Place> found = placeRepository.findAllById(req.getPlaceIds())
                .stream().collect(Collectors.toMap(Place::getPlaceId, p -> p));
        List<Place> selected = new ArrayList<>(req.getPlaceIds().size());
        for (Long id : req.getPlaceIds()) {
            Place p = found.get(id);
            if (p == null) throw new PlaceException(ErrorStatus.PLACE_NOT_FOUND);
            selected.add(p);
        }

        // 3) GPT용 좌표 리스트 구성 (입력 인덱스 = originalIndex)
        List<CoordinateDto> coordsForPlan = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Place p = selected.get(i);
            coordsForPlan.add(CoordinateDto.builder()
                    .lat(p.getLatitude())
                    .lng(p.getLongitude())
                    .originalIndex(i)
                    .placeId(p.getPlaceId())
                    .build());
        }

        // 4) 방문 순서 최적화 (GPT)
        SequencePlanner.PlanResult plan;
        try {
            plan = sequencePlanner.plan(coordsForPlan);
        } catch (Exception e) {
            List<Integer> fallback = IntStream.range(0, coordsForPlan.size()).boxed().toList();
            plan = new SequencePlanner.PlanResult(fallback, 0);
        }
        List<Integer> orderIdx = plan.order(); // 프론트 핵심: 입력 인덱스 기준 순서

        // placeId 기준 순서 리스트도 생성 (옵션)
        List<Long> orderedPlaceIds = orderIdx.stream()
                .map(i -> selected.get(i).getPlaceId())
                .toList();

        // 5) TMAP으로 총 거리/시간/요금 합계
        Totals totals = computeTotals(selected, orderedPlaceIds);
        int totalSec   = totals.totalDurationSec();
        int totalMin   = Math.max(1, totalSec / 60);
        int totalMeter = totals.totalDistanceMeters();
        int totalFare  = totals.totalFare();

        // 6) Route 저장 (새 Place 저장 없음)
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
                .totalDurationMinutes(totalMin) // 분
                .totalDistance(totalMeter) // m 저장
                .totalCost(totalFare)
                .themes(Collections.emptyList())
                .routeType(RouteType.AI)
                .build();
        routeRepository.save(route);

        // 7) RoutePlace 저장 (기존 Place만 연결)
        int visitOrder = 1;
        for (Integer idx : orderIdx) {
            Place p = selected.get(idx);
            routePlaceRepository.save(RoutePlace.builder()
                    .route(route)
                    .place(p)
                    .visitOrder(visitOrder++)
                    .recommendDurationMinutes(60)
                    .build());
        }

        // 8) 응답
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
                .order(orderIdx)                   // [2,0,1] 같은 입력 인덱스 순서
                .orderedPlaceIds(orderedPlaceIds)  // [303,101,202] 같은 실제 placeId 순서
                .build();
    }

    private Totals computeTotals(List<Place> selected, List<Long> orderedPlaceIds) {
        Map<Long, Place> byId = selected.stream()
                .collect(Collectors.toMap(Place::getPlaceId, p -> p));

        int sec = 0, dist = 0, fare = 0;

        for (int i = 0; i < orderedPlaceIds.size() - 1; i++) {
            Place from = byId.get(orderedPlaceIds.get(i));
            Place to   = byId.get(orderedPlaceIds.get(i + 1));

            TransitMetricsDto m = transitClient.metrics(
                    from.getLatitude(), from.getLongitude(),
                    to.getLatitude(),   to.getLongitude(),
                    null
            );
            sec  += m.getDurationSeconds();
            dist += m.getDistanceMeters();
            fare += m.getFare();
        }
        return new Totals(sec, dist, fare);
    }

    private record Totals(int totalDurationSec, int totalDistanceMeters, int totalFare) {
    }

    public RouteRecommendListResponseDto getRecommendedRoutes(Theme theme, int limit, int offset) {
        List<Route> allRoutes;

        if (theme != null) {
            // theme을 JSON 배열 문자열로 변환 (단일)
            String themesJson = "[\"" + theme.name() + "\"]";
            allRoutes = routeRepository.findPopularByThemes(themesJson);
        } else {
            allRoutes = routeRepository.findByRouteType(RouteType.POPULAR);
        }

        // 전체 개수
        int totalCount = allRoutes.size();

        // 수동 페이지네이션
        List<Route> paginatedRoutes = allRoutes.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        // DTO 변환
        List<RouteRecommendResponseDto> routes = paginatedRoutes.stream()
                .map(this::convertToRouteRecommendDto)
                .toList();

        return RouteRecommendListResponseDto.builder()
                .routes(routes)
                .totalCount(totalCount)
                .build();
    }

    private RouteRecommendResponseDto convertToRouteRecommendDto(Route route) {
        return RouteRecommendResponseDto.builder()
                .id(route.getId())
                .imageUrl(route.getImageUrl())
                .titleKo(route.getTitleKo())
                .titleEn(route.getTitleEn())
                .regionNameKo(route.getRegion() != null ? route.getRegion().getNameKo() : null)
                .regionNameEn(route.getRegion() != null ? route.getRegion().getNameEn() : null)
                .descriptionKo(route.getDescriptionKo())
                .descriptionEn(route.getDescriptionEn())
                .themes(route.getThemes())
                .build();

    }

    private List<LocalTime> getAvailableTimes() {
        return Arrays.asList(
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0)
        );
    }

    public RouteDetailResponseDto getRouteDetail(Long routeId, LocalDate date, LocalTime startTime) {
        // 1. 루트 엔티티 조회 (없으면 예외)
        Route route = findRouteById(routeId);

        // 2. 해당 루트에 속한 장소 리스트 조회 (방문 순서대로 정렬)
        List<RoutePlace> routePlaces = routePlaceRepository.findByRouteIdOrderByVisitOrder(routeId);

        // 3. 조회 결과를 DTO로 변환 (목데이터 생성 부분 제거)
        List<RouteDetailResponseDto.RoutePlaceDto> placeDtos = convertToRoutePlaceDtos(routePlaces, startTime);

        // 4. 최종 응답 객체 생성
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
                .imageUrl(route.getImageUrl())
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
                .address(routePlace.getPlace().getAddressKo())
                .imageUrls(Arrays.asList(routePlace.getPlace().getImageUrl()))
                .openingHours(routePlace.getPlace().getOpeningHours())
                .build();
    }

    private Region chooseRegionByMajority(List<Place> places) {

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