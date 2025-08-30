package com.mey.backend.domain.route.entity;

import com.mey.backend.domain.common.entity.BaseTimeEntity;
import com.mey.backend.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "route_places")
@Getter
@Setter
@NoArgsConstructor
public class RoutePlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(nullable = false)
    private Integer visitOrder;

    @Column(nullable = false)
    private Integer recommendDurationMinutes;

    @Column
    private LocalTime arrivalTime;

    @Column
    private LocalTime departureTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder
    public RoutePlace(Route route, Place place, Integer visitOrder, Integer recommendDurationMinutes) {
        this.route = route;
        this.place = place;
        this.visitOrder = visitOrder;
        this.recommendDurationMinutes = recommendDurationMinutes;
    }
}