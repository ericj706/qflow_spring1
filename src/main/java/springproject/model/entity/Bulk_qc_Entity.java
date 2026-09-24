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
@Table (name = "bulk_qc")
@AllArgsConstructor @NoArgsConstructor @Builder @Data 
public class Bulk_qc_Entity extends BaseTime{
    // 검사번호 PK
    @Id 
    @Column(name = "qc_id", length = 50)
    private String qc_id;

    // 생산 LOT 번호 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batches_Entity batchesEntity;

    // 검사시간
    @Column(name = "sample_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime sample_time;

    // 담당자 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users_Entity usersEntity;

    // 측정 pH
    @Column(name = "ph_measured", precision = 6, scale = 3)
    private BigDecimal ph_measured;

    // pH 합격범위
    @Column(name = "ph_criteria", length = 30)
    private String ph_criteria;

    // 측정 점도
    @Column(name = "viscosity_measured", precision = 12, scale = 3)
    private BigDecimal viscosity_measured;

    // 점도 합격범위
    @Column(name = "viscosity_criteria", length = 30)
    private String viscosity_criteria;

    // 측정 비중
    @Column(name = "specific_gravity", precision = 8, scale = 4)
    private BigDecimal specific_gravity;

    // 비중 합격범위
    @Column(name = "sg_criteria", length = 30)
    private String sg_criteria;

    // 외관검사
    @Column(name = "appearance_code", length = 50)
    private String appearance_code;

    // 미세기포검사
    @Column(name = "microbubble_code", length = 50)
    private String microbubble_code;

    // 미생물 검사
    @Column(name = "microbial_cfu")
    private Integer microbial_cfu;

    // 종합 검사결과
    @Column(name = "overall_qc_result", length = 40)
    private String overall_qc_result;

    // 검사 비고
    @Column(name = "qc_notes_code", length = 100)
    private String qc_notes_code;

    // 데이터 출처
    @Column(name = "record_source", length = 20)
    private String record_source;
    
}
