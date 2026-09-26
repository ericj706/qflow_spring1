package springproject.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Filling_packaging_Dto {
    private String pouch_id;
    private String batchId;
    private Integer userId;
    private String packaging_line;
    private LocalDateTime timestamp;
    private String sheet_material_code;
    private String sheet_lot_no;
    private BigDecimal sheet_dry_weight_g;
    private BigDecimal fill_weight_1st_g;
    private BigDecimal fill_weight_2nd_g;
    private BigDecimal essence_net_weight_g;
    private BigDecimal pouch_tare_weight_g;
    private BigDecimal gross_total_weight_g;
    private BigDecimal upper_seal_temp_c;
    private BigDecimal lower_seal_temp_c;
    private BigDecimal seal_pressure_bar;
    private BigDecimal n2_residual_o2_pct;
    private String checkweigher_status;
    private String metal_detector_status;
    private String vision_inspection_status;
    private String final_disposition;
    private String record_source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Filling_packaging_Entity toEntity(
            Batches_Entity batchesEntity,
            Users_Entity usersEntity) {

        return Filling_packaging_Entity.builder()
                .pouch_id(this.pouch_id)
                .batchesEntity(batchesEntity)
                .usersEntity(usersEntity)
                .packaging_line(this.packaging_line)
                .timestamp(this.timestamp)
                .sheet_material_code(this.sheet_material_code)
                .sheet_lot_no(this.sheet_lot_no)
                .sheet_dry_weight_g(this.sheet_dry_weight_g)
                .fill_weight_1st_g(this.fill_weight_1st_g)
                .fill_weight_2nd_g(this.fill_weight_2nd_g)
                .essence_net_weight_g(this.essence_net_weight_g)
                .pouch_tare_weight_g(this.pouch_tare_weight_g)
                .gross_total_weight_g(this.gross_total_weight_g)
                .upper_seal_temp_c(this.upper_seal_temp_c)
                .lower_seal_temp_c(this.lower_seal_temp_c)
                .seal_pressure_bar(this.seal_pressure_bar)
                .n2_residual_o2_pct(this.n2_residual_o2_pct)
                .checkweigher_status(this.checkweigher_status)
                .metal_detector_status(this.metal_detector_status)
                .vision_inspection_status(this.vision_inspection_status)
                .final_disposition(this.final_disposition)
                .record_source(this.record_source)
                .build();
    }

    // Entity → DTO
    public static Filling_packaging_Dto from(
            Filling_packaging_Entity entity) {

        return Filling_packaging_Dto.builder()
                .pouch_id(entity.getPouch_id())
                .batchId(
                        entity.getBatchesEntity() == null
                                ? null : entity.getBatchesEntity().getBatchId())
                .userId(
                        entity.getUsersEntity() == null
                                ? null : entity.getUsersEntity().getUserId())
                .packaging_line(entity.getPackaging_line())
                .timestamp(entity.getTimestamp())
                .sheet_material_code(entity.getSheet_material_code())
                .sheet_lot_no(entity.getSheet_lot_no())
                .sheet_dry_weight_g(entity.getSheet_dry_weight_g())
                .fill_weight_1st_g(entity.getFill_weight_1st_g())
                .fill_weight_2nd_g(entity.getFill_weight_2nd_g())
                .essence_net_weight_g(entity.getEssence_net_weight_g())
                .pouch_tare_weight_g(entity.getPouch_tare_weight_g())
                .gross_total_weight_g(entity.getGross_total_weight_g())
                .upper_seal_temp_c(entity.getUpper_seal_temp_c())
                .lower_seal_temp_c(entity.getLower_seal_temp_c())
                .seal_pressure_bar(entity.getSeal_pressure_bar())
                .n2_residual_o2_pct(entity.getN2_residual_o2_pct())
                .checkweigher_status(entity.getCheckweigher_status())
                .metal_detector_status(entity.getMetal_detector_status())
                .vision_inspection_status(entity.getVision_inspection_status())
                .final_disposition(entity.getFinal_disposition())
                .record_source(entity.getRecord_source())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
