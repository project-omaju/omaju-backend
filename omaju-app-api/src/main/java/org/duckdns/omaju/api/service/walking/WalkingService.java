package org.duckdns.omaju.api.service.walking;

import org.duckdns.omaju.api.dto.response.DataResponseDTO;

public interface WalkingService {
    DataResponseDTO<?> walkingTrails(double lat, double lon);
}