package com.mey.backend.domain.place.repository;

import com.mey.backend.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByNameKoContainingIgnoreCase(String keyword);

    @Query(value = "SELECT * FROM places WHERE JSON_CONTAINS(themes, :themeJson) LIMIT :limit", nativeQuery = true)
    List<Place> findByThemeKeywordWithLimit(@Param("themeJson") String themeJson, @Param("limit") int limit);

    @Query(value = """
        SELECT *
        FROM places
        ORDER BY 6371000 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(latitude)) *
            COS(RADIANS(longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
        ) ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Place> findNearest(@Param("lat") double lat, @Param("lng") double lng);
}

