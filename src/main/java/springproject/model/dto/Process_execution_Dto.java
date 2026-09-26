package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Process_execution_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
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
    public Process_execution_Entity toEntity(
            Batches_Entity batchesEntity) {

        return Process_execution_Entity.builder()
                // execution_id 제외
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
    public static Process_execution_Dto from(
            Process_execution_Entity entity) {

        return Process_execution_Dto.builder()
                .execution_id(entity.getExecution_id())
                .batchId(
                        entity.getBatchesEntity() == null
                                ? null : entity.getBatchesEntity().getBatchId())
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
    
}
