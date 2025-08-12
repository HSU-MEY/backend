package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    // 테마별 루트 조회
    List<Route> findByTheme(Theme theme);
    
    // 지역별 루트 조회  
    @Query("SELECT r FROM Route r WHERE r.region.nameKo = :regionName")
    List<Route> findByRegionName(@Param("regionName") String regionName);
    
    // 테마와 지역으로 필터링된 루트 조회
    @Query("SELECT r FROM Route r WHERE (:theme IS NULL OR r.theme = :theme) AND (:regionName IS NULL OR r.region.nameKo = :regionName)")
    Page<Route> findByThemeAndRegion(@Param("theme") Theme theme, @Param("regionName") String regionName, Pageable pageable);
    
    // 인기도순 정렬 (비용 기준으로 대체)
    @Query("SELECT r FROM Route r ORDER BY r.cost ASC")
    List<Route> findAllOrderByPopularity();
}