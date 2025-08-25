package com.mey.backend.domain.itinerary.service;

import com.mey.backend.domain.itinerary.dto.*;
import com.mey.backend.domain.itinerary.entity.Itinerary;
import com.mey.backend.domain.itinerary.entity.ItineraryStop;
import com.mey.backend.domain.itinerary.repository.ItineraryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    // private final SequencePlanner planner;


    public CreateItineraryResponse createItinerary(CreateItineraryRequest req) {

        // 더미 Itinerary ID
        Long dummyId = 1L;

        // 요청 받은 좌표들을 그대로 OrderedStopDto로 매핑
        List<OrderedStopDto> orderedStops = IntStream.range(0, req.getPlaces().size())
                .mapToObj(i -> OrderedStopDto.builder()
                        .order(i)                       // 단순히 입력 순서대로
                        .originalIndex(i)               // originalIndex도 i로
                        .coord(req.getPlaces().get(i))  // 입력 좌표 그대로
                        .build())
                .collect(Collectors.toList());

        // 요약 정보 더미
        ItinerarySummaryDto summary = ItinerarySummaryDto.builder()
                .totalDistanceMeters(12345)   // 12.3 km
                .totalDurationSeconds(3600)   // 1시간
                .totalCost(5000)              // 5000원
                .summaryText("총 12.3km, 약 1시간 소요 예상")
                .build();

        // 최종 응답 조립
        return CreateItineraryResponse.builder()
                .itineraryId(dummyId)
                .orderedStops(orderedStops)
                .summary(summary)
                .build();
    }


//    public CreateItineraryResponse createItinerary2(CreateItineraryRequest req) {
//        if (req == null || req.getPlaces() == null || req.getPlaces().size() < 2)
//            throw new IllegalArgumentException("places는 최소 2개 이상이어야 합니다.");
//        if (req.getPlaceCount() != req.getPlaces().size())
//            throw new IllegalArgumentException("placeCount와 places 길이가 일치해야 합니다.");
//        if (req.getPlaces().stream().anyMatch(p -> p.getLat() == null || p.getLng() == null))
//            throw new IllegalArgumentException("모든 좌표에 lat/lng가 필요합니다.");
//
//        IntStream.range(0, req.getPlaces().size()).forEach(i -> {
//            var c = req.getPlaces().get(i);
//            if (c.getOriginalIndex() == null) c.setOriginalIndex(i);
//        });
//
//        SequencePlanner.PlanResult plan = planner.plan(req.getPlaces());
//
//        Itinerary it = Itinerary.builder()
//                .totalDistanceMeters(plan.totalDistanceMeters())
//                .totalDurationSeconds(plan.totalDurationSeconds())
//                .totalCost(plan.totalCost())
//                .summaryText(plan.summaryText())
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        for (int order = 0; order < plan.order().size(); order++) {
//            int idx = plan.order().get(order);
//            CoordinateDto c = req.getPlaces().get(idx);
//
//            it.getStops().add(
//                    ItineraryStop.builder()
//                            .itinerary(it)
//                            .orderIndex(order)
//                            .originalIndex(c.getOriginalIndex())
//                            .lat(c.getLat())
//                            .lng(c.getLng())
//                            .placeId(c.getPlaceId())
//                            .name(c.getName())
//                            .build()
//            );
//        }
//
//        Itinerary saved = itineraryRepository.save(it);
//
//        List<OrderedStopDto> orderedStops = saved.getStops().stream()
//                .sorted(Comparator.comparingInt(ItineraryStop::getOrderIndex))
//                .map(s -> OrderedStopDto.builder()
//                        .order(s.getOrderIndex())
//                        .originalIndex(s.getOriginalIndex())
//                        .coord(CoordinateDto.builder()
//                                .lat(s.getLat())
//                                .lng(s.getLng())
//                                .placeId(s.getPlaceId())
//                                .name(s.getName())
//                                .originalIndex(s.getOriginalIndex())
//                                .build())
//                        .build())
//                .toList();
//
//        ItinerarySummaryDto summary = ItinerarySummaryDto.builder()
//                .totalDistanceMeters(saved.getTotalDistanceMeters())
//                .totalDurationSeconds(saved.getTotalDurationSeconds())
//                .totalCost(saved.getTotalCost())
//                .summaryText(saved.getSummaryText())
//                .build();
//
//        return CreateItineraryResponse.builder()
//                .itineraryId(saved.getId())
//                .orderedStops(orderedStops)
//                .summary(summary)
//                .build();
//    }
}