package springproject.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Measurement_result_Dto {
    // 판정한 원본 테이블
    private String sourceTable;

    // 판정한 원본 기록의 PK
    private String sourceRecordId;

    // 적용한 규칙번호
    private Integer ruleId;

    // 공정명
    private String processCode;

    // 판정 항목
    private String sensorName;

    // 비교에 사용한 값
    // 원료 칭량에서는 실제 무게가 아닌 오차율 또는 오차 무게
    private BigDecimal measuredValue;

    // 비교값의 단위
    private String unit;

    // 정상·주의·심각
    private String severity;

    
}
