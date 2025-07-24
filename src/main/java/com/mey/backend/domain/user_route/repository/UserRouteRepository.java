package com.mey.backend.domain.user_route.repository;

import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user_route.entity.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    List<UserRoute> findAllByUser(User user);

    Optional<UserRoute> findByUserAndStatus(User user, String status);
}