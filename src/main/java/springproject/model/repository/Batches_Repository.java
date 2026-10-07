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
import springproject.model.entity.Batches_Entity;

@Repository
public interface Batches_Repository
        extends JpaRepository<Batches_Entity, String> {

    // 전체조회 + 조건검색 + 페이징
    @Query("""
        SELECT b
        FROM Batches_Entity b
        LEFT JOIN b.usersEntity u
        WHERE (:startAt IS NULL OR b.startTime >= :startAt)
        AND (:endAtExclusive IS NULL OR b.startTime < :endAtExclusive)

        AND (:batchId IS NULL OR b.batchId = :batchId)

        AND (
            :keyword IS NULL
            OR b.batchId LIKE CONCAT('%', :keyword, '%')
            OR b.productName LIKE CONCAT('%', :keyword, '%')
        )

        AND (:productCode IS NULL OR b.productCode = :productCode)
        AND (:status IS NULL OR b.status = :status)
        AND (:userId IS NULL OR u.userId = :userId)
        AND (:tankId IS NULL OR b.tankId = :tankId)

        ORDER BY b.startTime DESC
        """)
    Page<Batches_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("batchId") String batchId,
            @Param("keyword") String keyword,
            @Param("productCode") String productCode,
            @Param("status") String status,
            @Param("userId") Integer userId,
            @Param("tankId") String tankId,
            Pageable pageable
    );
    
    // 공정 시작 처리 중 같은 LOT에 대한 동시 요청을 순서대로 처리  --> 한쪽에서 처리시 반대쪽 중복 요청 방지용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Batches_Entity b
            WHERE b.batchId = :batchId
            """)
    Optional<Batches_Entity> findByIdForUpdate(@Param("batchId") String batchId);
}