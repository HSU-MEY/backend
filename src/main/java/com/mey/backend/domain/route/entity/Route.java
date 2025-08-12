package com.mey.backend.domain.route.entity;

import com.mey.backend.domain.common.entity.BaseTimeEntity;
import com.mey.backend.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
public class Route extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(nullable = false)
    private String titleKo;

    @Column(nullable = false)
    private String titleEn;

    @Column(nullable = false)
    private String descriptionKo;

    @Column(nullable = false)
    private String descriptionEn;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int cost;

    @Column(nullable = false)
    private int totalDurationMinutes;

    @Column(nullable = false)
    private double totalDistance;

    @Column(nullable = false)
    private double totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteType routeType;
}
