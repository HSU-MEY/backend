package com.mey.backend.domain.place.entity;

import com.mey.backend.domain.common.entity.BaseTimeEntity;
import com.mey.backend.domain.region.entity.Region;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false)
    private String nameKo;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionKo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String address;

    private String contactInfo;

    private String websiteUrl;

    private String kakaoPlaceId;

    private String tourApiPlaceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> openingHours; // 예: "monday": "09:00-18:00"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<String> themes;

    @Column(nullable = false)
    private String costInfo;
}
