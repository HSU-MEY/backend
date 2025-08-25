package com.mey.backend.domain.itinerary.repository;

import com.mey.backend.domain.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}