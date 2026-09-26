package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Sensor_telemetry_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Sensor_telemetry_Dto {
    private Long sensor_id;
    private Long execution_id;
    private LocalDateTime timestamp;
    private BigDecimal paddle_rpm;
    private BigDecimal homomixer_rpm;
    private BigDecimal bulk_viscosity_cps;
    private BigDecimal ph_level;
    private BigDecimal motor_torque_pct;
    private BigDecimal vacuum_kpa;
    private BigDecimal cooling_valve_pct;
    private String record_source;
    private Integer userId;

    // DTO → Entity
    public Sensor_telemetry_Entity toEntity(
            Process_execution_Entity process_ExecutionEntity,
            Users_Entity usersEntity) {

        return Sensor_telemetry_Entity.builder()
                .process_ExecutionEntity(process_ExecutionEntity)
                .timestamp(this.timestamp)
                .paddle_rpm(this.paddle_rpm)
                .homomixer_rpm(this.homomixer_rpm)
                .bulk_viscosity_cps(this.bulk_viscosity_cps)
                .ph_level(this.ph_level)
                .motor_torque_pct(this.motor_torque_pct)
                .vacuum_kpa(this.vacuum_kpa)
                .cooling_valve_pct(this.cooling_valve_pct)
                .record_source(this.record_source)
                .usersEntity(usersEntity)
                .build();
    }
    
    // Entity → DTO
    public static Sensor_telemetry_Dto from(
            Sensor_telemetry_Entity entity) {

        return Sensor_telemetry_Dto.builder()
                .sensor_id(entity.getSensor_id())
                .execution_id(
                        entity.getProcess_ExecutionEntity() == null
                                ? null : entity.getProcess_ExecutionEntity().getExecution_id())
                .timestamp(entity.getTimestamp())
                .paddle_rpm(entity.getPaddle_rpm())
                .homomixer_rpm(entity.getHomomixer_rpm())
                .bulk_viscosity_cps(entity.getBulk_viscosity_cps())
                .ph_level(entity.getPh_level())
                .motor_torque_pct(entity.getMotor_torque_pct())
                .vacuum_kpa(entity.getVacuum_kpa())
                .cooling_valve_pct(entity.getCooling_valve_pct())
                .record_source(entity.getRecord_source())
                .userId(
                        entity.getUsersEntity() == null
                                ? null : entity.getUsersEntity().getUserId())
                .build();
    }
}
