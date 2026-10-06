package springproject.model.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Data_change_log_Entity;

@Repository 
public interface Data_change_log_Repository extends JpaRepository<Data_change_log_Entity, Long> {
     // 전체조회 + 조건검색 + 페이징
    @Query("""
            SELECT d
            FROM Data_change_log_Entity d
            LEFT JOIN d.usersEntity u
            WHERE (:startAt IS NULL OR d.changed_at >= :startAt)
              AND (:endAtExclusive IS NULL OR d.changed_at < :endAtExclusive)
              AND (:tableName IS NULL OR d.table_name = :tableName)
              AND (:recordId IS NULL OR d.record_id = :recordId)
              AND (
                    :recordIdKeyword IS NULL
                    OR LOWER(d.record_id)
                        LIKE CONCAT('%', LOWER(:recordIdKeyword), '%')
                  )
              AND (:columnName IS NULL OR d.column_name = :columnName)
              AND (:changeType IS NULL OR d.change_type = :changeType)
              AND (:userId IS NULL OR u.userId = :userId)
            ORDER BY d.changed_at DESC, d.change_id DESC
            """)
    Page<Data_change_log_Entity> search(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAtExclusive") LocalDateTime endAtExclusive,
            @Param("tableName") String tableName,
            @Param("recordId") String recordId,
            @Param("recordIdKeyword") String recordIdKeyword,
            @Param("columnName") String columnName,
            @Param("changeType") String changeType,
            @Param("userId") Integer userId,
            Pageable pageable
    );
}
