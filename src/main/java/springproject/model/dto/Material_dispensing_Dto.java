package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Material_dispensing_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Material_dispensing_Dto {
    private String dispenseId;
    private String batchId;
    private String materialCode;
    private String materialName;
    private String rawMaterialLot;
    private BigDecimal targetQtyKg;
    private BigDecimal actualQtyKg;
    private Integer userId;
    private LocalDateTime dispensedAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Material_dispensing_Entity toEntity(
            Batches_Entity batchEntity,
            Users_Entity usersEntity) {
        return Material_dispensing_Entity.builder()
                .dispenseId(this.dispenseId)
                .batchEntity(batchEntity)
                .materialCode(this.materialCode)
                .materialName(this.materialName)
                .rawMaterialLot(this.rawMaterialLot)
                .targetQtyKg(this.targetQtyKg)
                .actualQtyKg(this.actualQtyKg)
                .usersEntity(usersEntity)
                .dispensedAt(this.dispensedAt)
                .status(this.status)
                .build();
    }

    // Entity → DTO
    public static Material_dispensing_Dto from(
            Material_dispensing_Entity entity) {
                
        return Material_dispensing_Dto.builder()
                .dispenseId(entity.getDispenseId())
                .batchId(
                        entity.getBatchEntity() == null
                                ? null : entity.getBatchEntity().getBatchId())
                .materialCode(entity.getMaterialCode())
                .materialName(entity.getMaterialName())
                .rawMaterialLot(entity.getRawMaterialLot())
                .targetQtyKg(entity.getTargetQtyKg())
                .actualQtyKg(entity.getActualQtyKg())
                .userId(
                        entity.getUsersEntity() == null
                                ? null : entity.getUsersEntity().getUserId())
                .dispensedAt(entity.getDispensedAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
}
