package springproject.model.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    // PK연관관계--------------------
    @OneToMany(mappedBy = "usersEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Batches_Entity> batchesList = new ArrayList<>();

    @OneToMany(mappedBy = "usersEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Material_dispensing_Entity> materialDispensingList = new ArrayList<>();

    @OneToMany(mappedBy = "usersEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Bulk_qc_Entity> bulkQcList = new ArrayList<>();

    @OneToMany(mappedBy = "users_Entity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Anomaly_event_Entity> anomalyEventList = new ArrayList<>();

    @OneToMany(mappedBy = "usersEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Data_change_log_Entity> dataChangeLogList = new ArrayList<>();

    @OneToMany(mappedBy = "usersEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Filling_packaging_Entity> fillingPackagingList = new ArrayList<>();
}
