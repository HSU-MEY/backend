package com.mey.backend.domain.itinerary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itinerary_stops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItineraryStop {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    private Integer orderIndex;      // 방문 순서 (0..N-1)
    private Integer originalIndex;   // 입력 당시 순번

    private Double lat;
    private Double lng;

    private Long placeId;            // 선택
    private String name;             // 선택
}