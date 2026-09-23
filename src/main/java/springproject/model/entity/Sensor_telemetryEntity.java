package springproject.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "sensor_telemetry")
@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Sensor_telemetryEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long sensor_id;

    @ManyToOne 
    @JoinColumn (name = "execution_id", referencedColumnName = "execution_id")
    private Process_executionEntity process_ExecutionEntity;

    @Column(name = "timestamp", columnDefinition = "DATETIME(3)")
    private LocalDateTime timestamp;

    @Column(name = "paddle_rpm", precision = 10, scale = 2)
    private BigDecimal paddle_rpm;

    @Column(name = "homomixer_rpm", precision = 10, scale = 2)
    private BigDecimal homomixer_rpm;

    @Column(name = "bulk_viscosity_cps", precision = 12, scale = 3)
    private BigDecimal bulk_viscosity_cps;

    @Column(name = "ph_level", precision = 6, scale = 3)
    private BigDecimal ph_level;

    @Column(name = "motor_torque_pct", precision = 6, scale = 2)
    private BigDecimal motor_torque_pct;

    @Column(name = "vacuum_kpa", precision = 8, scale = 3)
    private BigDecimal vacuum_kpa;

    @Column(name = "cooling_valve_pct", precision = 6, scale = 2)
    private BigDecimal cooling_valve_pct;

    @Column(name = "record_source", length = 20)
    private String record_source;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UsersEntity userEntity;
}
