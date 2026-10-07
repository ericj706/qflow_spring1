package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Sensor_telemetry_Entity;

@Repository 
public interface Sensor_telemetry_Repository extends JpaRepository<Sensor_telemetry_Entity, Long> {
    // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT s
            FROM Sensor_telemetry_Entity s
            LEFT JOIN s.process_ExecutionEntity p
            LEFT JOIN p.batchesEntity b
            LEFT JOIN s.usersEntity u
            WHERE (:startAt IS NULL OR s.timestamp >= :startAt)
              AND (:endAt IS NULL OR s.timestamp <= :endAt)
              AND (:batchId IS NULL OR b.batchId = :batchId)
              AND (:executionId IS NULL OR p.execution_id = :executionId)
              AND (:processCode IS NULL OR p.process_code = :processCode)
              AND (:userId IS NULL OR u.userId = :userId)
            ORDER BY s.timestamp DESC, s.sensor_id DESC
            """)
    Page<Sensor_telemetry_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("batchId") String batchId,
            @Param("executionId") Long executionId,
            @Param("processCode") String processCode,
            @Param("userId") Integer userId,
            Pageable pageable
    );
    List<Sensor_telemetry_Entity> findByExecutionIdInOrderByTimestampAsc(List<Long> executionIds);
}
