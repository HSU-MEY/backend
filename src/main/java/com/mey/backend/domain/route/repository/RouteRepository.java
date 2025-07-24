package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}