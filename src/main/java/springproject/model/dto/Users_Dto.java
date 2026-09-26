package springproject.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import springproject.model.entity.Users_Entity;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Users_Dto {
    private Integer userId;

    // 비밀번호 해시: JSON 요청·응답 및 toString 출력에서 제외
    @JsonIgnore 
    @ToString.Exclude
    private String passwordHash;

    private String userName;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Dto -> Entity
    public Users_Entity toEntity(){
        return Users_Entity.builder()
                .passwordHash(this.passwordHash)
                .userName(this.userName)
                .role(this.role)
                .isActive(this.isActive).build();
    }

    // Entity → DTO   // 조회에서 비밀번호는 제외
    public static Users_Dto from(Users_Entity entity){
        return Users_Dto.builder()
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .role(entity.getRole())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt()).build();
    }

    
}
