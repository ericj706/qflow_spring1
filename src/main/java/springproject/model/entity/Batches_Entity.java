package springproject.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import springproject.model.entity.Users_Entity;

@Entity
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batches_Entity extends BaseTime{
    // 생산 LOT 번호 (VARCHAR(30), PK)
    @Id
    @Column(name = "batch_id", length = 30)
    private String batchId;

    // 제품코드 (VARCHAR(30))
    @Column(name = "product_code", length = 30)
    private String productCode;

    // 제품명 (VARCHAR(100))
    @Column(name = "product_name", length = 100)
    private String productName;

    // 목표 제조량 (DECIMAL(12,4))
    @Column(name = "target_bulk_kg", precision = 12, scale = 4)
    private BigDecimal targetBulkKg;

    // 실제 제조량 (DECIMAL(12,4))
    @Column(name = "actual_bulk_kg", precision = 12, scale = 4)
    private BigDecimal actualBulkKg;

    // 목표 생산수량 (INT)
    @Column(name = "target_units")
    private Integer targetUnits;

    // 실제 양품수량
    @Column(name = "actual_units")
    private Integer actualUnits;

    // 불량수량
    @Column(name = "defect_units")
    private Integer defectUnits;

    // 생산 시작시간
    @Column(name = "start_time")
    private LocalDateTime startTime;

    // 생산 종료시간
    @Column(name = "end_time")
    private LocalDateTime endTime;

    // 생산상태
    @Column(name = "status", length = 30)
    private String status;

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users_Entity usersEntity;

    // 제조 탱크번호
    @Column(name = "tank_id", length = 30)
    private String tankId;

    // 데이터 출처
    @Column(name = "record_source", length = 20)
    private String recordSource;
}