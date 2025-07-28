package com.mey.backend.domain.place.repository;

import com.mey.backend.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByNameKoContainingIgnoreCase(String keyword);
}