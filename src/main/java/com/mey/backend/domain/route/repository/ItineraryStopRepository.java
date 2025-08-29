package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
    List<ItineraryStop> findByItineraryIdOrderByOrderIndexAsc(Long itineraryId);
}