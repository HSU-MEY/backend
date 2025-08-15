package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    // 특정 테마를 포함하는 루트 조회
    @Query("SELECT r FROM Route r WHERE JSON_CONTAINS(r.themes, :theme)")
    List<Route> findByThemesContaining(@Param("theme") String theme);
    
    // 지역별 루트 조회  
    @Query("SELECT r FROM Route r WHERE r.region.nameKo = :regionName")
    List<Route> findByRegionName(@Param("regionName") String regionName);
    
    // 테마와 지역으로 필터링된 루트 조회
    @Query("SELECT r FROM Route r WHERE (:themes IS NULL OR :themes IS EMPTY OR EXISTS (SELECT 1 FROM Route r2 WHERE r2.id = r.id AND JSON_OVERLAPS(r2.themes, :themes))) AND (:regionName IS NULL OR r.region.nameKo = :regionName)")
    Page<Route> findByThemesAndRegion(@Param("themes") List<String> themes, @Param("regionName") String regionName, Pageable pageable);
    
    // 여러 테마 중 하나라도 포함하는 루트 조회
    @Query("SELECT r FROM Route r WHERE JSON_OVERLAPS(r.themes, :themes)")
    List<Route> findByThemesContainingAny(@Param("themes") List<String> themes);
    
    // 인기도순 정렬 (비용 기준으로 대체)
    @Query("SELECT r FROM Route r ORDER BY r.cost ASC")
    List<Route> findAllOrderByPopularity();
}