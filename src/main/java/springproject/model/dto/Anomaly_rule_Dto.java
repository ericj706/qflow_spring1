package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Anomaly_rule_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Anomaly_rule_Dto {
    private Integer ruleId;
    private String processCode;
    private String anomalyType;
    private String sensorName;
    private String conditionType;
    private BigDecimal warningMin;
    private BigDecimal warningMax;
    private BigDecimal criticalMin;
    private BigDecimal criticalMax;
    private String statusValue;
    private Integer durationSeconds;
    private String checkItems;
    private String responseDescription;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Anomaly_rule_Entity toEntity() {
        return Anomaly_rule_Entity.builder()
                .processCode(this.processCode)
                .anomalyType(this.anomalyType)
                .sensorName(this.sensorName)
                .conditionType(this.conditionType)
                .warningMin(this.warningMin)
                .warningMax(this.warningMax)
                .criticalMin(this.criticalMin)
                .criticalMax(this.criticalMax)
                .statusValue(this.statusValue)
                .durationSeconds(this.durationSeconds)
                .checkItems(this.checkItems)
                .responseDescription(this.responseDescription)
                .isActive(this.isActive)
                .build();
    }

    // Entity → DTO
    public static Anomaly_rule_Dto from(Anomaly_rule_Entity entity) {
        return Anomaly_rule_Dto.builder()
                .ruleId(entity.getRuleId())
                .processCode(entity.getProcessCode())
                .anomalyType(entity.getAnomalyType())
                .sensorName(entity.getSensorName())
                .conditionType(entity.getConditionType())
                .warningMin(entity.getWarningMin())
                .warningMax(entity.getWarningMax())
                .criticalMin(entity.getCriticalMin())
                .criticalMax(entity.getCriticalMax())
                .statusValue(entity.getStatusValue())
                .durationSeconds(entity.getDurationSeconds())
                .checkItems(entity.getCheckItems())
                .responseDescription(entity.getResponseDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
