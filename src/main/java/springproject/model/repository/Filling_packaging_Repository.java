package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.dto.Chart_Dto;

import java.util.List;

@Repository
public interface Filling_packaging_Repository extends JpaRepository<Filling_packaging_Entity, String> {

    // 1. 일별 집계 (날짜 검색 추가)
    @Query(value = """
        SELECT 
            DATE_FORMAT(timestamp, '%Y-%m-%d') AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED', 'PASS', 'OK') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR timestamp >= CONCAT(:startDate, ' 00:00:00'))
          AND (:endDate IS NULL OR :endDate = '' OR timestamp <= CONCAT(:endDate, ' 23:59:59'))
        GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d') ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findDailySummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 2. 시간별 집계 (날짜 검색 추가)
    @Query(value = """
        SELECT 
            DATE_FORMAT(timestamp, '%Y-%m-%d %H:00') AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED', 'PASS', 'OK') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR timestamp >= CONCAT(:startDate, ' 00:00:00'))
          AND (:endDate IS NULL OR :endDate = '' OR timestamp <= CONCAT(:endDate, ' 23:59:59'))
        GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d %H:00') ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findHourlySummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 3. LOT(배치)별 집계 (날짜 검색 추가) 
    @Query(value = """
        SELECT 
            batch_id AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED', 'PASS', 'OK') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE (:startDate IS NULL OR :startDate = '' OR timestamp >= CONCAT(:startDate, ' 00:00:00'))
          AND (:endDate IS NULL OR :endDate = '' OR timestamp <= CONCAT(:endDate, ' 23:59:59'))
        GROUP BY batch_id ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> findLotSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 4. 한 LOT내에 15분 단위
    @Query(value = """
        SELECT 
            DATE_FORMAT(
                DATE_SUB(timestamp, INTERVAL MINUTE(timestamp) % 15 MINUTE), '%Y-%m-%d %H:%i'
            ) AS timeGroup,
            COUNT(CASE WHEN final_disposition IN ('DISP_ACCEPTED', 'PASS', 'OK') THEN 1 END) AS passCount,
            COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) AS failCount,
            ROUND((COUNT(CASE WHEN final_disposition NOT IN ('DISP_ACCEPTED', 'PASS', 'OK') OR final_disposition IS NULL THEN 1 END) / COUNT(*)) * 100, 2) AS defectRate
        FROM filling_packaging
        WHERE batch_id = :batchId
        AND (:startDate IS NULL OR :startDate = '' OR timestamp >= CONCAT(:startDate, ' 00:00:00'))
        AND (:endDate IS NULL OR :endDate = '' OR timestamp <= CONCAT(:endDate, ' 23:59:59'))
        GROUP BY timeGroup ORDER BY timeGroup ASC
        """, nativeQuery = true)
    List<Chart_Dto> find15minSummary(
        @Param("batchId") String batchId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate);
}