package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Anomaly_event_Entity;

@Repository 
public interface Anomaly_event_Repository extends JpaRepository<Anomaly_event_Entity, Long> {
    // 전체조회 + 조건검색 + 페이징
    // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT a
            FROM Anomaly_event_Entity a
            LEFT JOIN a.batches_Entity b
            LEFT JOIN a.users_Entity u
            WHERE (:startAt IS NULL OR a.occurredAt >= :startAt)
              AND (:endAtExclusive IS NULL OR a.occurredAt < :endAtExclusive)
              AND (:severity IS NULL OR a.severity = :severity)
              AND (:actionStatus IS NULL OR a.actionStatus = :actionStatus)
              AND (:batchId IS NULL OR b.batchId = :batchId)
              AND (
                    :batchIdKeyword IS NULL
                    OR LOWER(b.batchId)
                        LIKE CONCAT('%', LOWER(:batchIdKeyword), '%')
                  )
              AND (:processCode IS NULL OR a.processCode = :processCode)
              AND (:anomalyType IS NULL OR a.anomalyType = :anomalyType)
              AND (:userId IS NULL OR u.userId = :userId)
            ORDER BY a.occurredAt DESC, a.anomalyId DESC
            """)
    Page<Anomaly_event_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("severity") String severity,
            @Param("actionStatus") String actionStatus,
            @Param("batchId") String batchId,
            @Param("batchIdKeyword") String batchIdKeyword,
            @Param("processCode") String processCode,
            @Param("anomalyType") String anomalyType,
            @Param("userId") Integer userId,
            Pageable pageable
    );
}
