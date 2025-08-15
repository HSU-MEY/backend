package com.mey.backend.domain.user_route.repository;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user_route.entity.UserRouteStatus;
import com.mey.backend.domain.user_route.entity.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    List<UserRoute> findAllByUser(User user);
    
    List<UserRoute> findByUser(User user);
    
    List<UserRoute> findByUserOrderByCreatedAtDesc(User user);
    
    List<UserRoute> findByUserAndStatus(User user, UserRouteStatus userRouteStatus);
    
    List<UserRoute> findByUserAndStatusOrderByCreatedAtDesc(User user, UserRouteStatus userRouteStatus);

    Optional<UserRoute> findByUserRouteIdAndUser(Long userRouteId, User user);
    
    boolean existsByUserAndRoute(User user, Route route);
}
