package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.entity.RoutePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutePlaceRepository extends JpaRepository<RoutePlace, Long> {
    
    List<RoutePlace> findByRouteOrderByVisitOrder(Route route);
    
    List<RoutePlace> findByRouteIdOrderByVisitOrder(Long routeId);
}