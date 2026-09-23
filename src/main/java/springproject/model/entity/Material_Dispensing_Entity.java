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

@Entity
@Table(name = "material_dispensing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material_Dispensing_Entity {
    // 칭량번호 (VARCHAR(100), PK)
    @Id
    @Column(name = "dispense_id", length = 100)
    private String dispenseId;

    // 생산 LOT 번호 (batch_id, FK -> batches 테이블)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch_Entity batchEntity;

    // 원료코드 (VARCHAR(40))
    @Column(name = "material_code", length = 40)
    private String materialCode;

    // 원료명 (VARCHAR(150))
    @Column(name = "material_name", length = 150)
    private String materialName;

    // 원료 LOT 번호 (VARCHAR(50))
    @Column(name = "raw_material_lot", length = 50)
    private String rawMaterialLot;

    // 목표 투입량 (DECIMAL(12,4))
    @Column(name = "target_qty_kg", precision = 12, scale = 4)
    private BigDecimal targetQtyKg;

    // 실제 투입량 (DECIMAL(12,4))
    @Column(name = "actual_qty_kg", precision = 12, scale = 4)
    private BigDecimal actualQtyKg;

    // 담당자 (user_id, FK -> users 테이블)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users_Entity userEntity;

    // 칭량시간 (DATETIME(3))
    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    // 칭량상태 (VARCHAR(50))
    @Column(name = "status", length = 50)
    private String status;
}