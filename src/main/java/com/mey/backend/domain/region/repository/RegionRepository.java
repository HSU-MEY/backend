package com.mey.backend.domain.region.repository;

import com.mey.backend.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByNameKo(String nameKo);
}