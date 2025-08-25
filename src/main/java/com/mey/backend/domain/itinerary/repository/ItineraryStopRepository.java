package com.mey.backend.domain.itinerary.repository;

import com.mey.backend.domain.itinerary.entity.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
}