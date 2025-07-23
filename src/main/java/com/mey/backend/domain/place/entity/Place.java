package com.mey.backend.domain.place.entity;

import com.mey.backend.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(nullable = false)
    private String nameKo;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private String descriptionKo;

    @Column(nullable = false)
    private String descriptionEn;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String contactInfo;

    @Column(nullable = false)
    private String websiteUrl;

    @Column(nullable = false)
    private String kakaoPlaceId;

    @Column(nullable = false)
    private String tourApiPlaceId;

    @Column(nullable = false)
    private String openingHours; // json 타입으로 변환 필요

    @Column(nullable = false)
    private String themes; // json 타입으로 변환 필요

    @Column(nullable = false)
    private String tags;

    @Column(nullable = false)
    private String costInfo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}