package com.mey.backend.domain.place.repository;

import com.mey.backend.domain.place.entity.UserLikePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLikePlaceRepository extends JpaRepository<UserLikePlace, Long> {

    List<UserLikePlace> findByUserId(Long userId);
}