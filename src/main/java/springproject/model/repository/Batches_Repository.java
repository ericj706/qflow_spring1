package springproject.model.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Batches_Entity;

@Repository
public interface Batches_Repository
        extends JpaRepository<Batches_Entity, String> {

    @Query("""
        SELECT b
        FROM Batches_Entity b
        WHERE (:startAt IS NULL OR b.startTime >= :startAt)
        AND (:endAtExclusive IS NULL OR b.startTime < :endAtExclusive)
        AND (:productCode IS NULL OR b.productCode = :productCode)
        AND (:status IS NULL OR b.status = :status)
        ORDER BY b.startTime DESC
        """)
    Page<Batches_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("productCode") String productCode,
            @Param("status") String status,
            Pageable pageable
    );
}