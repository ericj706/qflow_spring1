package springproject.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "anomaly_rule")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Anomaly_rule_Entity extends BaseTime {
    
    // 규칙번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    // 공정코드
    @Column(name = "process_code", length = 30)
    private String processCode;

    // 이상 유형
    @Column(name = "anomaly_type", length = 50)
    private String anomalyType;

    // 검사 항목
    @Column(name = "sensor_name", length = 50)
    private String sensorName;

    // 조건 유형
    @Column(name = "condition_type", length = 20)
    private String conditionType;

    // 주의 하한
    @Column(name = "warning_min", precision = 12, scale = 3)
    private BigDecimal warningMin;

    // 주의 상한
    @Column(name = "warning_max", precision = 12, scale = 3)
    private BigDecimal warningMax;

    // 이상 하한
    @Column(name = "critical_min", precision = 12, scale = 3)
    private BigDecimal criticalMin;

    // 이상 상한
    @Column(name = "critical_max", precision = 12, scale = 3)
    private BigDecimal criticalMax;

    // 검사 상태값
    @Column(name = "status_value", length = 50)
    private String statusValue;

    // 지속시간 기준
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // 확인사항
    @Column(name = "check_items", columnDefinition = "TEXT")
    private String checkItems;

    // 대응 안내
    @Column(name = "response_description", columnDefinition = "TEXT")
    private String responseDescription;

    // 사용 여부
    @Column(name = "is_active")
    private Boolean isActive;

}
