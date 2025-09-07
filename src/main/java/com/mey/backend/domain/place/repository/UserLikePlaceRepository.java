package com.mey.backend.domain.place.repository;

import com.mey.backend.domain.place.entity.Place;
import com.mey.backend.domain.place.entity.UserLikePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserLikePlaceRepository extends JpaRepository<UserLikePlace, Long> {

    @Query("SELECT ulp.place FROM UserLikePlace ulp GROUP BY ulp.place ORDER BY COUNT(ulp.id) DESC")
    List<Place> findPopularPlaces();
}