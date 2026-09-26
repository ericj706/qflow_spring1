package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Batches_Dto {
    private String batchId;
    private String productCode;
    private String productName;
    private BigDecimal targetBulkKg;
    private BigDecimal actualBulkKg;
    private Integer targetUnits;
    private Integer actualUnits;
    private Integer defectUnits;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer userId;
    private String tankId;
    private String recordSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Batches_Entity toEntity(Users_Entity usersEntity){
        return Batches_Entity.builder()
                .batchId(this.batchId)
                .productCode(this.productCode)
                .productName(this.productName)
                .targetBulkKg(this.targetBulkKg)
                .actualBulkKg(this.actualBulkKg)
                .targetUnits(this.targetUnits)
                .actualUnits(this.actualUnits)
                .defectUnits(this.defectUnits)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .status(this.status)
                .usersEntity(usersEntity)
                .tankId(this.tankId)
                .recordSource(this.recordSource).build();
    }

     // Entity → DTO
    public static Batches_Dto from(Batches_Entity entity) {
        return Batches_Dto.builder()
                .batchId(entity.getBatchId())
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .targetBulkKg(entity.getTargetBulkKg())
                .actualBulkKg(entity.getActualBulkKg())
                .targetUnits(entity.getTargetUnits())
                .actualUnits(entity.getActualUnits())
                .defectUnits(entity.getDefectUnits())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .userId(
                        entity.getUsersEntity() == null
                                ? null : entity.getUsersEntity().getUserId()
                )
                .tankId(entity.getTankId())
                .recordSource(entity.getRecordSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
}
