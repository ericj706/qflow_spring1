package springproject.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springproject.model.entity.Data_change_log_Entity;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Data_change_log_Dto {
    private Long change_id;
    private String table_name;
    private String record_id;
    private String column_name;
    private String old_value;
    private String new_value;
    private String change_type;
    private Integer userId;
    private String change_reason;
    private LocalDateTime changed_at;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // DTO → Entity
    public Data_change_log_Entity toEntity(Users_Entity usersEntity) {
        return Data_change_log_Entity.builder()
                // change_id는 자동 증가이므로 제외
                .table_name(this.table_name)
                .record_id(this.record_id)
                .column_name(this.column_name)
                .old_value(this.old_value)
                .new_value(this.new_value)
                .change_type(this.change_type)
                .usersEntity(usersEntity)
                .change_reason(this.change_reason)
                .changed_at(this.changed_at)
                .build();
    }

    // Entity → DTO
    public static Data_change_log_Dto from(Data_change_log_Entity entity) {
        return Data_change_log_Dto.builder()
                .change_id(entity.getChange_id())
                .table_name(entity.getTable_name())
                .record_id(entity.getRecord_id())
                .column_name(entity.getColumn_name())
                .old_value(entity.getOld_value())
                .new_value(entity.getNew_value())
                .change_type(entity.getChange_type())
                .userId(
                        entity.getUsersEntity() == null
                                ? null: entity.getUsersEntity().getUserId())
                .change_reason(entity.getChange_reason())
                .changed_at(entity.getChanged_at())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
}
