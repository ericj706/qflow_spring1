package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Material_dispensing_Entity;

@Repository 
public interface  Material_dispensing_Repository extends JpaRepository<Material_dispensing_Entity, String> {
    // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT m
            FROM Material_dispensing_Entity m
            LEFT JOIN m.batchesEntity b
            LEFT JOIN m.usersEntity u
            WHERE (:startAt IS NULL
                   OR m.dispensedAt >= :startAt)
              AND (:endAtExclusive IS NULL
                   OR m.dispensedAt < :endAtExclusive)
              AND (:batchId IS NULL
                   OR b.batchId = :batchId)
              AND (:materialCode IS NULL
                   OR m.materialCode = :materialCode)
              AND (:materialName IS NULL
                   OR LOWER(m.materialName) LIKE
                      CONCAT('%', LOWER(:materialName), '%'))
              AND (:rawMaterialLot IS NULL
                   OR m.rawMaterialLot = :rawMaterialLot)
              AND (:status IS NULL
                   OR m.status = :status)
              AND (:userId IS NULL
                   OR u.userId = :userId)
            ORDER BY m.dispensedAt DESC, m.dispenseId DESC
            """)
    Page<Material_dispensing_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("batchId") String batchId,
            @Param("materialCode") String materialCode,
            @Param("materialName") String materialName,
            @Param("rawMaterialLot") String rawMaterialLot,
            @Param("status") String status,
            @Param("userId") Integer userId,
            Pageable pageable
    );
    List<Material_dispensing_Entity> findByBatchId(String batchId);
}
