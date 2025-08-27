package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
}