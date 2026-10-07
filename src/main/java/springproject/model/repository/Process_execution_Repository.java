package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Process_execution_Entity;

@Repository 
public interface Process_execution_Repository extends JpaRepository<Process_execution_Entity, Long> {
    // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT p
            FROM Process_execution_Entity p
            LEFT JOIN p.batchesEntity b
            WHERE (:startAt IS NULL
                   OR p.start_time >= :startAt)
              AND (:endAtExclusive IS NULL
                   OR p.start_time < :endAtExclusive)
              AND (:batchId IS NULL
                   OR b.batchId = :batchId)
              AND (:processCode IS NULL
                   OR p.process_code = :processCode)
              AND (:status IS NULL
                   OR p.status = :status)
            ORDER BY p.start_time DESC, p.execution_id DESC
            """)
    Page<Process_execution_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("batchId") String batchId,
            @Param("processCode") String processCode,
            @Param("status") String status,
            Pageable pageable
    );

    // 같은 LOT/공정/상태의 실행 기록이 존재하는지 확인 ->> 존재시 1보다 크게 나옴
    @Query ("""
              SELECT COUNT(p)
              FROM Process_execution_Entity p
              WHERE p.batchesEntity.batchId = :batchId
               AND p.process_code = :processCode
               AND p.status = :status
              """)
     long countByBatchAndProcessAndStatus(
          @Param("batchId") String batchId,
          @Param("processCode") String processCode,
          @Param("status") String status
     );
}
