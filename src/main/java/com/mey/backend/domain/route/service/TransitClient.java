package com.mey.backend.domain.route.service;

import com.mey.backend.domain.route.dto.TransitSegmentDto;
import java.time.LocalDateTime;

public interface TransitClient {
    TransitSegmentDto route(
            String fromName, double fromLat, double fromLng,
            String toName,   double toLat,   double toLng,
            LocalDateTime departureTime
    );

    default TransitSegmentDto route(double fromLat, double fromLng,
                                    double toLat,   double toLng,
                                    LocalDateTime departureTime) {
        return route("", fromLat, fromLng, "", toLat, toLng, departureTime);
    }
}