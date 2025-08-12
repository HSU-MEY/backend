package com.mey.backend.domain.user_route.entity;

import com.mey.backend.domain.common.entity.BaseTimeEntity;
import com.mey.backend.domain.route.entity.Route;
import com.mey.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "user_routes")
@Getter
@Setter
@NoArgsConstructor
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
    private Status status;

    @Column(nullable = false)
    private int currentPlaceOrder;

    @Column(nullable = false)
    private Date plannedStartDate;

    @Column(nullable = false)
    private LocalDateTime plannedStartTime;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;
}