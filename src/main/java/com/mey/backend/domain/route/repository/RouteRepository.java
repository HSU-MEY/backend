package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.RouteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    // 특정 테마를 포함하는 루트 조회
    @Query(value = "SELECT * FROM routes r WHERE JSON_CONTAINS(r.themes, JSON_QUOTE(:theme))", nativeQuery = true)
    List<Route> findByThemesContaining(@Param("theme") String theme);
    
    // 지역별 루트 조회  
    @Query("SELECT r FROM Route r WHERE r.region.nameKo = :regionName")
    List<Route> findByRegionName(@Param("regionName") String regionName);
    
    // 테마와 지역으로 필터링된 루트 조회
    @Query(value = "SELECT r.* FROM routes r LEFT JOIN regions reg ON r.region_id = reg.region_id WHERE (:themes IS NULL OR JSON_OVERLAPS(r.themes, CAST(:themes AS JSON))) AND (:regionName IS NULL OR reg.name_ko = :regionName)", nativeQuery = true)
    List<Route> findByThemesAndRegion(@Param("themes") String themes, @Param("regionName") String regionName);
    
    // 여러 테마 중 하나라도 포함하는 루트 조회
    @Query(value = "SELECT * FROM routes r WHERE JSON_OVERLAPS(r.themes, CAST(:themes AS JSON))", nativeQuery = true)
    List<Route> findByThemesContainingAny(@Param("themes") String themes);
    
    // 인기도순 정렬 (비용 기준으로 대체)
    @Query("SELECT r FROM Route r ORDER BY r.totalCost ASC")
    List<Route> findAllOrderByPopularity();


    // POPULAR 전체
    List<Route> findByRouteType(RouteType routeType);

    // POPULAR + themes 조건
    @Query(value = """
    SELECT *
    FROM routes r
    WHERE r.route_type = 'POPULAR'
      AND JSON_OVERLAPS(r.themes, CAST(:themesJson AS JSON))
    """, nativeQuery = true)
    List<Route> findPopularByThemes(@Param("themesJson") String themesJson);
}
