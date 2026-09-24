package springproject.model.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Data @Builder 
@AllArgsConstructor @NoArgsConstructor 
@EntityListeners(AuditingEntityListener.class)

@Table (name="data_change_log")
public class Data_change_log_Entity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_id")
    private Long change_id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String table_name;

    /*
     변경 데이터번호 (PK가 복합키이거나 문자열일 수 있어 VARCHAR 대응)
    */
    @Column(name = "record_id", nullable = false, length = 100)
    private String record_id;

    /*
     변경 항목 (컬럼명)
    */
    @Column(name = "column_name", length = 100)
    private String column_name;

    /*
     기존값
    */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String old_value;

    /*
     변경값
    */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String new_value;

    /*
     변경 유형 (예: INSERT, UPDATE, DELETE)
    */
    @Column(name = "change_type", length = 20)
    private String change_type;

    /*
     변경 작업자번호 (FK)
    */
    @JoinColumn(name="user_id") 
    @ManyToOne 
    @Column(name = "user_id")
    private Users_Entity usersEntity;

    /*
     변경사유
    */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String change_reason;

    
}
