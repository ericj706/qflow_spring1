package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Anomaly_event_Entity;
import springproject.model.entity.Anomaly_rule_Entity;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Anomaly_event_Dto {
        private Long anomalyId;
        private String batchId;
        private String pouchId;
        private Integer ruleId;
        private String sourceAlarmId;
        private String processCode;
        private String anomalyType;
        private String sensorName;
        private BigDecimal measuredValue;
        private String severity;
        private String alarmMessage;
        private LocalDateTime occurredAt;
        private LocalDateTime resolvedAt;
        private Integer durationSec;
        private String actionStatus;
        private String actionNote;
        private Integer userId;
        private LocalDateTime actionTime;
        private String sourceType;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // DTO → Entity
        public Anomaly_event_Entity toEntity(
                Batches_Entity batches_Entity,
                Filling_packaging_Entity filling_packaging_Entity,
                Anomaly_rule_Entity anomaly_rule_Entity,
                Users_Entity users_Entity) {

                return Anomaly_event_Entity.builder()
                        // anomalyId는 자동 증가이므로 제외
                        .batches_Entity(batches_Entity)
                        .filling_packaging_Entity(filling_packaging_Entity)
                        .anomaly_rule_Entity(anomaly_rule_Entity)
                        .sourceAlarmId(this.sourceAlarmId)
                        .processCode(this.processCode)
                        .anomalyType(this.anomalyType)
                        .sensorName(this.sensorName)
                        .measuredValue(this.measuredValue)
                        .severity(this.severity)
                        .alarmMessage(this.alarmMessage)
                        .occurredAt(this.occurredAt)
                        .resolvedAt(this.resolvedAt)
                        .durationSec(this.durationSec)
                        .actionStatus(this.actionStatus)
                        .actionNote(this.actionNote)
                        .users_Entity(users_Entity)
                        .actionTime(this.actionTime)
                        .sourceType(this.sourceType)
                        .build();
        }

        // Entity → DTO
        public static Anomaly_event_Dto from(Anomaly_event_Entity entity) {
                return Anomaly_event_Dto.builder()
                        .anomalyId(entity.getAnomalyId())
                        .batchId(
                                entity.getBatches_Entity() == null
                                        ? null : entity.getBatches_Entity().getBatchId())
                        .pouchId(
                                entity.getFilling_packaging_Entity() == null
                                        ? null: entity.getFilling_packaging_Entity().getPouch_id())
                        .ruleId(
                                entity.getAnomaly_rule_Entity() == null
                                        ? null: entity.getAnomaly_rule_Entity().getRuleId())
                        .sourceAlarmId(entity.getSourceAlarmId())
                        .processCode(entity.getProcessCode())
                        .anomalyType(entity.getAnomalyType())
                        .sensorName(entity.getSensorName())
                        .measuredValue(entity.getMeasuredValue())
                        .severity(entity.getSeverity())
                        .alarmMessage(entity.getAlarmMessage())
                        .occurredAt(entity.getOccurredAt())
                        .resolvedAt(entity.getResolvedAt())
                        .durationSec(entity.getDurationSec())
                        .actionStatus(entity.getActionStatus())
                        .actionNote(entity.getActionNote())
                        .userId(
                                entity.getUsers_Entity() == null
                                        ? null: entity.getUsers_Entity().getUserId())
                        .actionTime(entity.getActionTime())
                        .sourceType(entity.getSourceType())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build();
        }
}
