package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import springproject.model.entity.Anomaly_event_Entity;
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

     // 공정 종료 처리를 위해 실행 기록 조회 및 잠금  // 공정 실행 기록 한건 잠금
     @Lock (LockModeType.PESSIMISTIC_WRITE)
     @Query ("""
               SELECT p 
               FROM Process_execution_Entity p
               WHERE p.execution_id = :executionId
               """)
     Optional<Process_execution_Entity> findByIdForUpdate(@Param("executionId") Long executionId );

     // 공정 실행번호로 연결된 LOT번호 조회
     @Query ("""
               SELECT p.batchesEntity.batchId
               FROM Process_execution_Entity p
               WHERE p.execution_id = :executionId
          """)
     Optional<String> findBatchIdByExecutionId(@Param("executionId") Long executionId);
   
     // 해당 실행에서 이어서 생성된 다음 공정 조회
     @Query("""
               SELECT p
               FROM Process_execution_Entity p
               WHERE p.previousExecution.execution_id = :executionId
          """)
     Optional<Process_execution_Entity> findNextByExecutionId(@Param("executionId") Long executionId);
     
     // 해당 LOT에 공정 실행 이력이 있는지 확인
     @Query("""
          SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
          FROM Process_execution_Entity p
          WHERE p.batchesEntity.batchId = :batchId
          """)
     boolean existsByBatchesEntity_BatchId(@Param("batchId") String batchId);

          // 최종 승인 대상으로 지정된 검사인지 확인
     @Query("""
          SELECT COUNT(p)
          FROM Process_execution_Entity p
          WHERE p.finalBulkQc.qc_id = :qcId
          """)
     long countByFinalQcId(@Param("qcId") String qcId);

     // 담당 중인 공정 수 확인
     // 진행 중이거나, 종료 후 다음 공정으로 넘어가지 못한 실행을 포함
     @Query("""
          SELECT COUNT(p)
          FROM Process_execution_Entity p
          WHERE p.managerUser.userId = :userId
               AND (p.status = '진행중'
               OR (p.status = '완료'
                    AND p.batchesEntity.status = '진행중'
                    AND NOT EXISTS (
                         SELECT n.execution_id
                         FROM Process_execution_Entity n
                         WHERE n.previousExecution = p
                    )))
          """)
     long countAssignedProcesses(@Param("userId") Integer userId);

}
