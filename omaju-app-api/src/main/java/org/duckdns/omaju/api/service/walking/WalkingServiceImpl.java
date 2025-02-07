package org.duckdns.omaju.api.service.walking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duckdns.omaju.api.dto.response.DataResponseDTO;
import org.duckdns.omaju.api.dto.response.walking.WalkingHistoryResponseDTO;
import org.duckdns.omaju.api.dto.response.walking.WalkingTrailsDTO;
import org.duckdns.omaju.core.entity.member.Member;
import org.duckdns.omaju.core.entity.walking.WalkingHistory;
import org.duckdns.omaju.core.entity.walking.WalkingTrails;
import org.duckdns.omaju.core.repository.MemberRepository;
import org.duckdns.omaju.core.repository.WalkingHistoryRepository;
import org.duckdns.omaju.core.repository.WalkingTrailsRepository;
import org.duckdns.omaju.core.util.HTTPUtils;
import org.duckdns.omaju.core.util.JSONUtils;
import org.duckdns.omaju.core.util.network.Header;
import org.duckdns.omaju.core.util.network.Post;
import org.duckdns.omaju.core.util.network.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingServiceImpl implements WalkingService {
    private final WalkingTrailsRepository walkingTrailsRepository;
    private final WalkingHistoryRepository walkingHistoryRepository;
    private final MemberRepository memberRepository;

    @Value("${tmap.key}")
    private String API_KEY;
    private final String VERSION = "1";
    private final String CALLBACK = "application/json";

    @Override
    public DataResponseDTO<?> walkingTrails(double lat, double lon) {
        WalkingTrails easy = walkingTrailsRepository.findRandomTrailByEasyLevel().orElse(null);
        WalkingTrails normal = walkingTrailsRepository.findRandomTrailByNormalLevel().orElse(null);
        WalkingTrails hard = walkingTrailsRepository.findRandomTrailByHardLevel().orElse(null);

        return DataResponseDTO.builder()
                .data(Arrays.asList(
                        WalkingTrailsDTO.builder()
                                .id(easy.getId())
                                .name(easy.getName())
                                .level("하")
                                .startLat(easy.getStartLat())
                                .startLon(easy.getStartLon())
                                .endLat(easy.getEndLat())
                                .endLon(easy.getEndLon())
                                .build()
                        ,WalkingTrailsDTO.builder()
                                .id(normal.getId())
                                .name(normal.getName())
                                .level("중")
                                .startLat(normal.getStartLat())
                                .startLon(normal.getStartLon())
                                .endLat(normal.getEndLat())
                                .endLon(normal.getEndLon())
                                .build(),
                        WalkingTrailsDTO.builder()
                                .id(hard.getId())
                                .name(hard.getName())
                                .level("상")
                                .startLat(hard.getStartLat())
                                .startLon(hard.getStartLon())
                                .endLat(hard.getEndLat())
                                .endLon(hard.getEndLon())
                                .build()))
                .message("산책로 목록이 정상적으로 조회되었습니다.")
                .statusName(HttpStatus.OK.name())
                .status(HttpStatus.OK.value())
                .build();
    }

    @Override
    public DataResponseDTO<?> tmapTrace(double startLat, double startLon, double endLat, double endLon) {
        String url = String.format("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=%s&callback=%s&appKey=%s", VERSION, CALLBACK, API_KEY);

        Header header = new Header()
                .append("User-Agent", HTTPUtils.USER_AGENT)
                .append("Accept-Language", HTTPUtils.ACCEPT_LANGUAGE)
                .append("Connection", HTTPUtils.CONNECTION)
                .append("Content-Type", HTTPUtils.CONTENT_TYPE_JSON);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("startX", startLon);
        jsonObject.addProperty("startY", startLat);
        jsonObject.addProperty("endX", endLon);
        jsonObject.addProperty("endY", endLat);
        jsonObject.addProperty("startName", "시작");
        jsonObject.addProperty("endName", "종료");

        RequestBody requestBody = new RequestBody(jsonObject);

        try {
            Post post = new Post(url)
                    .setHeader(header)
                    .setRequestBody(requestBody)
                    .execute();

            int responseCode = post.getResponseCode();
            if (responseCode != org.apache.http.HttpStatus.SC_OK) {
                log.debug("responseCode: {}", responseCode);
                throw new RuntimeException("외부 API 통신 오류: " + post.getUrl());
            }

            List<List<Double>> result = new ArrayList<>();

            JsonObject obj = JSONUtils.parse(post.getResult());
            JsonArray features = obj.getAsJsonArray("features");

            for (Object o: features) {
                JsonObject feature = (JsonObject) o;
                JsonObject geometry = feature.get("geometry").getAsJsonObject();
                String type = geometry.get("type").getAsString();

                if (type.equals("LineString")) {
                    JsonArray coordinates = geometry.get("coordinates").getAsJsonArray();
                    for (Object oo: coordinates) {
                        JsonArray coordinate = (JsonArray) oo;

                        List<Double> coordinateList = new ArrayList<>();
                        coordinateList.add(coordinate.get(1).getAsDouble());
                        coordinateList.add(coordinate.get(0).getAsDouble());

                        result.add(coordinateList);
                    }
                }
            }

            return DataResponseDTO.builder()
                    .data(result)
                    .message("도보 경로가 정상적으로 조회되었습니다.")
                    .statusName(HttpStatus.OK.name())
                    .status(HttpStatus.OK.value())
                    .build();
        } catch(Exception e) {
            e.printStackTrace();
            log.error("확인되지 않은 오류가 발생했습니다");
            throw new RuntimeException();
        }
    }

    @Override
    public DataResponseDTO<?> historyInsert(Member member, double distance, int steps) {
        WalkingHistory walkingHistory = WalkingHistory.builder()
                .member(member)
                .distance(distance)
                .steps(steps)
                .build();
        walkingHistoryRepository.save(walkingHistory);

        return DataResponseDTO.builder()
                .data(null)
                .message("산책 히스토리가 정상적으로 저장되었습니다.")
                .statusName(HttpStatus.OK.name())
                .status(HttpStatus.OK.value())
                .build();
    }

    @Override
    public DataResponseDTO<?> walkingHistoryByDate(Member member, LocalDate date) {
        long startOfDayMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endOfDayMillis = date.atTime(23, 59, 59, 999999999).toInstant(ZoneOffset.UTC).toEpochMilli();

        List<WalkingHistory> walkingHistories = walkingHistoryRepository.findByMemberIdAndDateRange(member.getId(), startOfDayMillis, endOfDayMillis);
        List<WalkingHistoryResponseDTO> walkingHistoryDTO = walkingHistories.stream()
                .map(wh -> WalkingHistoryResponseDTO.builder()
                        .id(wh.getId())
                        .distance(wh.getDistance())
                        .steps(wh.getSteps())
                        .build())
                .toList();

        return DataResponseDTO.builder()
                .data(walkingHistoryDTO)
                .message("일별 산책 히스토리 목록이 정상적으로 조회되었습니다.")
                .statusName(HttpStatus.OK.name())
                .status(HttpStatus.OK.value())
                .build();
    }

    @Override
    public DataResponseDTO<?> walkingHistoryByMonth(Member member, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);  // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.atEndOfMonth();  // 해당 월의 마지막 날

        long startOfDayMillis = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endOfDayMillis = endDate.atTime(23, 59, 59, 999999999).toInstant(ZoneOffset.UTC).toEpochMilli();

        List<WalkingHistory> walkingHistories = walkingHistoryRepository.findByMemberIdAndDateRange(member.getId(), startOfDayMillis, endOfDayMillis);

        Map<LocalDate, Boolean> dateMap = new HashMap<>();

        // 해당 월의 모든 날짜를 false로 초기화
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, false);
        }

        // 이벤트가 있는 날짜를 true로 설정
        for (WalkingHistory wh : walkingHistories) {
            LocalDate eventDate = Instant.ofEpochMilli(wh.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (dateMap.containsKey(eventDate)) {
                dateMap.put(eventDate, true);
            }
        }

        return DataResponseDTO.builder()
                .data(dateMap)
                .message("월별 산책 히스토리 여부가 정상적으로 조회되었습니다.")
                .statusName(HttpStatus.OK.name())
                .status(HttpStatus.OK.value())
                .build();
    }
}
