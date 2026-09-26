package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Bulk_qc_Dto {
    private String qc_id;
    private String batchId;
    private LocalDateTime sample_time;
    private Integer userId;
    private BigDecimal ph_measured;
    private String ph_criteria;
    private BigDecimal viscosity_measured;
    private String viscosity_criteria;
    private BigDecimal specific_gravity;
    private String sg_criteria;
    private String appearance_code;
    private String microbubble_code;
    private Integer microbial_cfu;
    private String overall_qc_result;
    private String qc_notes_code;
    private String record_source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Bulk_qc_Entity toEntity(
            Batches_Entity batchesEntity,
            Users_Entity usersEntity) {

        return Bulk_qc_Entity.builder()
                .qc_id(this.qc_id)
                .batchesEntity(batchesEntity)
                .sample_time(this.sample_time)
                .usersEntity(usersEntity)
                .ph_measured(this.ph_measured)
                .ph_criteria(this.ph_criteria)
                .viscosity_measured(this.viscosity_measured)
                .viscosity_criteria(this.viscosity_criteria)
                .specific_gravity(this.specific_gravity)
                .sg_criteria(this.sg_criteria)
                .appearance_code(this.appearance_code)
                .microbubble_code(this.microbubble_code)
                .microbial_cfu(this.microbial_cfu)
                .overall_qc_result(this.overall_qc_result)
                .qc_notes_code(this.qc_notes_code)
                .record_source(this.record_source)
                .build();
    }

    // Entity → DTO
    public static Bulk_qc_Dto from(Bulk_qc_Entity entity) {
        return Bulk_qc_Dto.builder()
                .qc_id(entity.getQc_id())
                .batchId(
                        entity.getBatchesEntity() == null
                                ? null : entity.getBatchesEntity().getBatchId())
                .sample_time(entity.getSample_time())
                .userId(
                        entity.getUsersEntity() == null
                                ? null : entity.getUsersEntity().getUserId())
                .ph_measured(entity.getPh_measured())
                .ph_criteria(entity.getPh_criteria())
                .viscosity_measured(entity.getViscosity_measured())
                .viscosity_criteria(entity.getViscosity_criteria())
                .specific_gravity(entity.getSpecific_gravity())
                .sg_criteria(entity.getSg_criteria())
                .appearance_code(entity.getAppearance_code())
                .microbubble_code(entity.getMicrobubble_code())
                .microbial_cfu(entity.getMicrobial_cfu())
                .overall_qc_result(entity.getOverall_qc_result())
                .qc_notes_code(entity.getQc_notes_code())
                .record_source(entity.getRecord_source())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
}
