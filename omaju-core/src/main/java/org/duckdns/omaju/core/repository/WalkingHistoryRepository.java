package org.duckdns.omaju.core.repository;

import org.duckdns.omaju.core.entity.walking.WalkingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkingHistoryRepository extends JpaRepository<WalkingHistory, Integer> {
    @Query("SELECT wh FROM WalkingHistory wh WHERE wh.member.id = :memberId AND wh.createdAt BETWEEN :startOfDayMillis AND :endOfDayMillis")
    List<WalkingHistory> findByMemberIdAndDateRange(int memberId, long startOfDayMillis, long endOfDayMillis);
}
