package springproject.model.repository;


import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;  // 조회 결과 목록과 전체 건수·전체 페이지 수를 담는 타입
// org.hibernate.query.Page          Hibernate에서 조회할 페이지 범위를 지정하는 타입
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Bulk_qc_Entity;

@Repository 
public interface Bulk_qc_Repository extends JpaRepository<Bulk_qc_Entity, String> {

        // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT q
            FROM Bulk_qc_Entity q
            LEFT JOIN q.batchesEntity b
            LEFT JOIN q.usersEntity u
            WHERE (:startAt IS NULL OR q.sample_time >= :startAt)
              AND (:endAtExclusive IS NULL OR q.sample_time < :endAtExclusive)
              AND (:batchId IS NULL OR b.batchId = :batchId)
              AND (:overallQcResult IS NULL OR q.overall_qc_result = :overallQcResult)
              AND (:userId IS NULL OR u.userId = :userId)
            ORDER BY q.sample_time DESC, q.qc_id ASC
            """)
    Page<Bulk_qc_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("batchId") String batchId,
            @Param("overallQcResult") String overallQcResult,
            @Param("userId") Integer userId,
            Pageable pageable
    );
    
}
