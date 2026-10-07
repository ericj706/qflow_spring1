package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Process_execution_Entity;

@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
@Data 
public class Process_execution_Dto {
    private Long execution_id;
    private String batchId;
    private String process_code;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private BigDecimal duration_min;
    private String status;
    private String record_source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Process_execution_Entity toEntity(Batches_Entity batchesEntity) {
        return Process_execution_Entity.builder()
                .batchesEntity(batchesEntity)
                .process_code(this.process_code)
                .start_time(this.start_time)
                .end_time(this.end_time)
                .duration_min(this.duration_min)
                .status(this.status)
                .record_source(this.record_source)
                .build();
    }

    // Entity → DTO
    public static Process_execution_Dto from(Process_execution_Entity entity) {
        return Process_execution_Dto.builder()
                .execution_id(entity.getExecution_id())
                .batchId(entity.getBatchesEntity() == null ? null : entity.getBatchesEntity().getBatchId())
                .process_code(entity.getProcess_code())
                .start_time(entity.getStart_time())
                .end_time(entity.getEnd_time())
                .duration_min(entity.getDuration_min())
                .status(entity.getStatus())
                .record_source(entity.getRecord_source())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ==========================================
    // 대시보드 전용 Inner DTO 클래스
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessStageStatus {
        private String stageName;
        private String batchId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String currentSubProcess;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RealtimeTelemetry {
        private String batchId;
        private Long executionId;
        private String currentProcessName;
        private BigDecimal tankTempC;
        private BigDecimal phLevel;
        private BigDecimal bulkViscosityCps;
        private BigDecimal paddleRpm;
        private BigDecimal homomixerRpm;
        private BigDecimal motorTorquePct;
        private BigDecimal vacuumKpa;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardResponse {
        private String activeBatchId;
        private List<String> batchList;
        private List<ProcessStageStatus> stages;
        private RealtimeTelemetry latestTelemetry;
        private List<RealtimeTelemetry> telemetryHistory;
    }
}