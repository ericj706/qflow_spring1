package springproject.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity 
@Table (name = "filling_packaging")
@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Filling_packaging_Entity extends BaseTime{
    @Id 
    @Column (name = "pouch_id", length = 100)
    private String pouch_id;

    // 생산 LOT 번호 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batches_Entity batchesEntity;

    // 담당자 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users_Entity usersEntity;

    // 공정 실행번호 FK — 새로 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    @ToString.Exclude
    private Process_execution_Entity process_execution_Entity;

    // 포장라인
    @Column(name = "packaging_line", length = 40)
    private String packaging_line;

    // 포장·검사시간
    @Column(name = "timestamp", columnDefinition = "DATETIME(3)")
    private LocalDateTime timestamp;

    // 시트 자재코드
    @Column(name = "sheet_material_code", length = 50)
    private String sheet_material_code;

    // 시트 LOT 번호
    @Column(name = "sheet_lot_no", length = 50)
    private String sheet_lot_no;

     // 시트 중량
    @Column(name = "sheet_dry_weight_g", precision = 8, scale = 3)
    private BigDecimal sheet_dry_weight_g;

    // 1차 충진량
    @Column(name = "fill_weight_1st_g", precision = 8, scale = 3)
    private BigDecimal fill_weight_1st_g;

    // 2차 충진량
    @Column(name = "fill_weight_2nd_g", precision = 8, scale = 3)
    private BigDecimal fill_weight_2nd_g;

    // 에센스 순중량
    @Column(name = "essence_net_weight_g", precision = 8, scale = 3)
    private BigDecimal essence_net_weight_g;

    // 파우치 자체중량
    @Column(name = "pouch_tare_weight_g", precision = 8, scale = 3)
    private BigDecimal pouch_tare_weight_g;

    // 완제품 총중량
    @Column(name = "gross_total_weight_g", precision = 8, scale = 3)
    private BigDecimal gross_total_weight_g;

    // 상부 실링온도
    @Column(name = "upper_seal_temp_c", precision = 8, scale = 3)
    private BigDecimal upper_seal_temp_c;

    // 하부 실링온도
    @Column(name = "lower_seal_temp_c", precision = 8, scale = 3)
    private BigDecimal lower_seal_temp_c;

    // 실링압력
    @Column(name = "seal_pressure_bar", precision = 8, scale = 3)
    private BigDecimal seal_pressure_bar;

    // 잔존 산소비율
    @Column(name = "n2_residual_o2_pct", precision = 6, scale = 3)
    private BigDecimal n2_residual_o2_pct;

    // 중량검사 결과
    @Column(name = "checkweigher_status", length = 40)
    private String checkweigher_status;

    // 금속검사 결과
    @Column(name = "metal_detector_status", length = 40)
    private String metal_detector_status;

    // 비전검사 결과
    @Column(name = "vision_inspection_status", length = 40)
    private String vision_inspection_status;

    // 최종 판정
    @Column(name = "final_disposition", length = 40)
    private String final_disposition;

    // 비고
    @Column(name = "note", length = 100)
    private String note;

    // 데이터 출처
    @Column(name = "record_source", length = 20)
    private String record_source;

    @OneToMany (mappedBy = "filling_packaging_Entity", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Anomaly_event_Entity> anomalyEventList = new ArrayList<>();

    // 제품별 폐기 상태
    // null: 폐기 대상 아님 또는 판정 전
    // 폐기대기: 불합격 제품의 폐기 확정 대기
    // 폐기완료: 관리자가 폐기를 확정함
    @Column(name = "disposal_status", length = 20)
    private String disposalStatus;

    // 제품 폐기를 확정한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposed_by_user_id")
    @ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Users_Entity disposedByUser;

    // 제품 폐기 확정 시간
    @Column(name = "disposed_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime disposedAt;

    
}
