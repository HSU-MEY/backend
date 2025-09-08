package com.mey.backend.domain.place.repository;

import com.mey.backend.domain.place.entity.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByNameKoContainingIgnoreCaseOrNameEnContainingIgnoreCase(String nameKo, String nameEn);

    @Query(value = "SELECT * FROM places WHERE JSON_CONTAINS(themes, JSON_QUOTE(:keyword), '$') LIMIT :limit", nativeQuery = true)
    List<Place> findByThemeKeywordWithLimit(@Param("keyword") String keyword, @Param("limit") int limit);

    // JPQL의 MOD 함수로 홀수 placeId만 필터링
    @Query("SELECT p FROM Place p WHERE MOD(p.placeId, 2) = 1")
    List<Place> findOddIdPlaces(Pageable pageable);

    Optional<Place> findByNameKo(String nameKo);

    @Query(value = "SELECT COUNT(*) FROM places p JOIN regions r ON p.region_id = r.region_id WHERE JSON_CONTAINS(p.themes, :themeJson) AND r.name_ko = :regionName", nativeQuery = true)
    int countByThemeAndRegion(@Param("themeJson") String themeJson, @Param("regionName") String regionName);
}

