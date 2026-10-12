package springproject.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Filling_packaging_Save_Dto {
    // 개별 제품번호
    private String pouch_id;
    // 최종 포장 검사 공정 실행번호
    private Long executionId;
    // 작업 담당자 번호
    private Integer userId;
    // 포장라인
    private String packaging_line;
    // 포장·검사시간
    private LocalDateTime timestamp;
    // 시트 자재코드
    private String sheet_material_code;
    // 시트 LOT 번호
    private String sheet_lot_no;
    // 시트 중량
    private BigDecimal sheet_dry_weight_g;
    // 1차 충진량
    private BigDecimal fill_weight_1st_g;
    // 2차 충진량
    private BigDecimal fill_weight_2nd_g;
    // 에센스 순중량
    private BigDecimal essence_net_weight_g;
    // 파우치 자체중량
    private BigDecimal pouch_tare_weight_g;
    // 완제품 총중량
    private BigDecimal gross_total_weight_g;
    // 상부 실링온도
    private BigDecimal upper_seal_temp_c;
    // 하부 실링온도
    private BigDecimal lower_seal_temp_c;
    // 실링압력
    private BigDecimal seal_pressure_bar;
    // 잔존 산소비율
    private BigDecimal n2_residual_o2_pct;
    // 중량검사 결과
    private String checkweigher_status;
    // 금속검사 결과
    private String metal_detector_status;
    // 비전검사 결과
    private String vision_inspection_status;
    // 최종 판정: 합격 / 불합격
    // 아직 판정하지 않았다면 null
    private String final_disposition;
}
