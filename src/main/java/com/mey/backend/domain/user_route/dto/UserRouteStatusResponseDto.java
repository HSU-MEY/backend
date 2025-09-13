package com.mey.backend.domain.user_route.dto;

import com.mey.backend.domain.user_route.entity.UserRoute;
import com.mey.backend.domain.user_route.entity.UserRouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRouteStatusResponseDto {

    private Long userRouteId;
    private UserRouteStatus status;

    public static UserRouteStatusResponseDto from(UserRoute userRoute) {
        return UserRouteStatusResponseDto.builder()
                .userRouteId(userRoute.getUserRouteId())
                .status(userRoute.getUserRouteStatus())
                .build();
    }
}