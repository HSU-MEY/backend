package com.mey.backend.domain.user_route.service;

import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.route.repository.RouteRepository;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user_route.dto.*;
import com.mey.backend.domain.user_route.entity.Status;
import com.mey.backend.domain.user_route.entity.UserRoute;
import com.mey.backend.domain.user_route.repository.UserRouteRepository;
import com.mey.backend.global.exception.RouteException;
import com.mey.backend.global.exception.UserRouteException;
import com.mey.backend.global.payload.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRouteService {

    private final UserRouteRepository userRouteRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public UserRouteSaveResponseDto saveUserRoute(User user, UserRouteSaveRequestDto request) {
        Route route = routeRepository.findById(request.getRouteId())
            .orElseThrow(() -> new RouteException(ErrorStatus.ROUTE_NOT_FOUND));

        // 중복 저장 확인
        boolean alreadyExists = userRouteRepository.existsByUserAndRoute(user, route);
        if (alreadyExists) {
            throw new UserRouteException(ErrorStatus.ROUTE_ALREADY_SAVED);
        }

        UserRoute userRoute = new UserRoute();
        userRoute.setUser(user);
        userRoute.setRoute(route);
        userRoute.setStatus(Status.NOT_STARTED);
        userRoute.setCurrentPlaceOrder(0);
        userRoute.setPlannedStartDate(java.sql.Date.valueOf(request.getPreferredStartDate()));
        userRoute.setPlannedStartTime(LocalDateTime.of(request.getPreferredStartDate(), request.getPreferredStartTime()));

        UserRoute savedUserRoute = userRouteRepository.save(userRoute);

        return UserRouteSaveResponseDto.builder()
            .success(true)
            .message("루트가 저장되었습니다")
            .savedRouteId(savedUserRoute.getUserRouteId())
            .build();
    }

    public UserRouteListResponseDto getUserRoutes(User user, String status) {
        List<UserRoute> userRoutes;
        
        if (status != null && !"all".equals(status)) {
            try {
                Status statusEnum = convertStringToStatus(status);
                userRoutes = userRouteRepository.findByUserAndStatusOrderByCreatedAtDesc(user, statusEnum);
            } catch (IllegalArgumentException e) {
                throw new UserRouteException(ErrorStatus.BAD_REQUEST);
            }
        } else {
            userRoutes = userRouteRepository.findByUserOrderByCreatedAtDesc(user);
        }

        List<UserRouteListResponseDto.SavedRouteDto> savedRoutes = userRoutes.stream()
            .map(this::convertToSavedRouteDto)
            .collect(Collectors.toList());

        return UserRouteListResponseDto.builder()
            .savedRoutes(savedRoutes)
            .build();
    }

    private Status convertStringToStatus(String status) {
        switch (status.toLowerCase()) {
            case "not_started":
                return Status.NOT_STARTED;
            case "ongoing":
                return Status.ON_GOING;
            case "completed":
                return Status.COMPLETED;
            default:
                throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Transactional
    public void updateUserRoute(User user, Long savedRouteId, UserRouteUpdateRequestDto request) {
        UserRoute userRoute = userRouteRepository.findByUserRouteIdAndUser(savedRouteId, user)
            .orElseThrow(() -> new UserRouteException(ErrorStatus.USER_ROUTE_NOT_FOUND));

        // 이미 시작된 루트는 수정할 수 없도록 제한
        if (userRoute.getStatus() != Status.NOT_STARTED) {
            throw new UserRouteException(ErrorStatus.BAD_REQUEST);
        }

        userRoute.setPlannedStartDate(java.sql.Date.valueOf(request.getPreferredStartDate()));
        userRoute.setPlannedStartTime(LocalDateTime.of(request.getPreferredStartDate(), request.getPreferredStartTime()));
        userRouteRepository.save(userRoute);
    }

    @Transactional
    public void deleteUserRoute(User user, Long savedRouteId) {
        UserRoute userRoute = userRouteRepository.findByUserRouteIdAndUser(savedRouteId, user)
            .orElseThrow(() -> new UserRouteException(ErrorStatus.USER_ROUTE_NOT_FOUND));
        
        userRouteRepository.delete(userRoute);
    }

    private UserRouteListResponseDto.SavedRouteDto convertToSavedRouteDto(UserRoute userRoute) {
        LocalDate plannedDate = userRoute.getPlannedStartDate() != null ? 
            LocalDate.of(userRoute.getPlannedStartDate().getYear() + 1900, 
                        userRoute.getPlannedStartDate().getMonth() + 1, 
                        userRoute.getPlannedStartDate().getDate()) : null;
        LocalDate today = LocalDate.now();
        
        return UserRouteListResponseDto.SavedRouteDto.builder()
            .savedRouteId(userRoute.getUserRouteId())
            .routeId(userRoute.getRoute().getId())
            .title(userRoute.getRoute().getTitleKo())
            .description(userRoute.getRoute().getDescriptionKo())
            .totalDurationMinutes(userRoute.getRoute().getTotalDurationMinutes())
            .preferredStartDate(plannedDate)
            .preferredStartTime(userRoute.getPlannedStartTime() != null ? userRoute.getPlannedStartTime().toLocalTime() : null)
            .isPastDate(plannedDate != null && plannedDate.isBefore(today))
            .daysUntilTrip(plannedDate != null ? (int) ChronoUnit.DAYS.between(today, plannedDate) : 0)
            .savedAt(userRoute.getCreatedAt())
            .build();
    }
}
