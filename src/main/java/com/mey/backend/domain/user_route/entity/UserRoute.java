package com.mey.backend.domain.user_route.entity;

import com.mey.backend.domain.common.entity.BaseTimeEntity;
import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Setter;

@Entity
@Table(name = "user_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserRoute extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRouteId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRouteStatus userRouteStatus;

    @Column(nullable = false)
    private int currentPlaceOrder;

    @Setter
    @Column(nullable = false)
    private Date plannedStartDate;

    @Setter
    @Column(nullable = false)
    private LocalDateTime plannedStartTime;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;
}
