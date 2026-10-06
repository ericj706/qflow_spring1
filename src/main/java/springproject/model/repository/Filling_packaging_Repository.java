package springproject.model.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.dto.chart.Chart_Dto;
import springproject.model.entity.Filling_packaging_Entity;


@Repository
public interface Filling_packaging_Repository extends JpaRepository<Filling_packaging_Entity, String> {

    // 1. 일별 집계
    @Query(value = """
        SELECT 
            DATE_FORMAT(timestamp, '%Y-%m-%d') AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') >= :startDate)
          AND (:endDate IS NULL OR :endDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') <= :endDate)
        GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d') 
        ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findDailySummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 2. 시간별 집계
    @Query(value = """
        SELECT 
            DATE_FORMAT(timestamp, '%Y-%m-%d %H:00') AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') >= :startDate)
          AND (:endDate IS NULL OR :endDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') <= :endDate)
        GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d %H:00')
        ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findHourlySummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 3. LOT(배치)별 집계
    @Query(value = """
        SELECT 
            batch_id AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') >= :startDate)
          AND (:endDate IS NULL OR :endDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') <= :endDate)
        GROUP BY batch_id 
        ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findLotSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 4. 한 LOT내에 15분 단위 집계
    @Query(value = """
        SELECT 
            DATE_FORMAT(
                DATE_SUB(timestamp, INTERVAL MINUTE(timestamp) % 15 MINUTE), '%Y-%m-%d %H:%i'
            ) AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE batch_id = :batchId
          AND (:startDate IS NULL OR :startDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') >= :startDate)
          AND (:endDate IS NULL OR :endDate = '' OR DATE_FORMAT(timestamp, '%Y-%m-%d') <= :endDate)
        GROUP BY timeGroup 
        ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> find15minSummary(
        @Param("batchId") String batchId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate);

    // 5. 목록 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT f
            FROM Filling_packaging_Entity f
            LEFT JOIN f.batchesEntity b
            LEFT JOIN f.usersEntity u
            WHERE (:startAt IS NULL OR f.timestamp >= :startAt)
              AND (:endAtExclusive IS NULL OR f.timestamp < :endAtExclusive)
              AND (:batchId IS NULL OR b.batchId = :batchId)
              AND (:packagingLine IS NULL OR f.packaging_line = :packagingLine)
              AND (:finalDisposition IS NULL OR f.final_disposition = :finalDisposition)
              AND (:checkweigherStatus IS NULL OR f.checkweigher_status = :checkweigherStatus)
              AND (:metalDetectorStatus IS NULL OR f.metal_detector_status = :metalDetectorStatus)
              AND (:visionInspectionStatus IS NULL OR f.vision_inspection_status = :visionInspectionStatus)
              AND (:userId IS NULL OR u.userId = :userId)
            ORDER BY f.timestamp DESC, f.pouch_id ASC
            """)
    Page<Filling_packaging_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("batchId") String batchId,
            @Param("packagingLine") String packagingLine,
            @Param("finalDisposition") String finalDisposition,
            @Param("checkweigherStatus") String checkweigherStatus,
            @Param("metalDetectorStatus") String metalDetectorStatus,
            @Param("visionInspectionStatus") String visionInspectionStatus,
            @Param("userId") Integer userId,
            Pageable pageable
    );
}