package org.duckdns.omaju.api.service.walking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.omaju.api.dto.response.DataResponseDTO;
import org.duckdns.omaju.core.entity.walking.WalkingTrails;
import org.duckdns.omaju.core.repository.WalkingTrailsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingServiceImpl implements WalkingService {
    private final WalkingTrailsRepository walkingTrailsRepository;

    @Override
    public DataResponseDTO<?> walkingTrails(double lat, double lon) {
        WalkingTrails easy = walkingTrailsRepository.findRandomTrailByEasyLevel().orElse(null);
        WalkingTrails normal = walkingTrailsRepository.findRandomTrailByNormalLevel().orElse(null);
        WalkingTrails hard = walkingTrailsRepository.findRandomTrailByHardLevel().orElse(null);

        return DataResponseDTO.builder()
                .data(Arrays.asList(easy, normal, hard))
                .message("산책로 목록이 정상적으로 조회되었습니다.")
                .statusName(HttpStatus.OK.name())
                .status(HttpStatus.OK.value())
                .build();
    }
}
