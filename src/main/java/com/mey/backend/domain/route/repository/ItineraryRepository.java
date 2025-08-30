package com.mey.backend.domain.route.repository;

import com.mey.backend.domain.route.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

@Deprecated
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}