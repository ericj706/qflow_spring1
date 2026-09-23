package springproject.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users_Entity extends BaseTime{
    // 사용자번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    // 비밀번호
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // 사용자명
    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;

    // 담당 역할
    @Column(name = "role", length = 30)
    private String role;

    // 계정상태
    @Column(name = "is_active")
    private Boolean isActive;
}
