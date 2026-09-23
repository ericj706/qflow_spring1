package springproject.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "anomaly_event")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Anomaly_event {
    // 이상 발생번호
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anomaly_id")
    private Long anomalyId;

    // 생산 LOT 번호 - FK
    @Column(name = "batch_id", length = 30)
    private String batchId;

    // 개별 제품번호 - FK, NULL 허용
    @Column(name = "pouch_id", length = 100)
    private String pouchId;

    // 규칙번호 - FK, NULL 허용
    @Column(name = "rule_id")
    private Integer ruleId;

    // 원본 알람번호 - UNIQUE, NULL 허용
    @Column(name = "source_alarm_id", length = 50, unique = true)
    private String sourceAlarmId;

    // 공정코드
    @Column(name = "process_code", length = 30)
    private String processCode;

    // 이상 유형
    @Column(name = "anomaly_type", length = 50)
    private String anomalyType;

    // 이상 측정항목
    @Column(name = "sensor_name", length = 50)
    private String sensorName;

    // 측정값
    @Column(name = "measured_value", precision = 12, scale = 3)
    private BigDecimal measuredValue;

    // 이상 심각도
    @Column(name = "severity", length = 30)
    private String severity;

    // 알람 메시지
    @Column(name = "alarm_message", columnDefinition = "TEXT")
    private String alarmMessage;

    // 이상 발생시간
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    // 이상 해제시간
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // 지속시간
    @Column(name = "duration_sec")
    private Integer durationSec;

    // 조치상태
    @Column(name = "action_status", length = 30)
    private String actionStatus;

    // 조치내용
    @Column(name = "action_note", columnDefinition = "TEXT")
    private String actionNote;

    // 조치 관리자번호 - FK
    @Column(name = "action_user_id")
    private Integer actionUserId;

    // 조치시간
    @Column(name = "action_time")
    private LocalDateTime actionTime;

    // 데이터 출처
    @Column(name = "source_type", length = 20)
    private String sourceType;

}
