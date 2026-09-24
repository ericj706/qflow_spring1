package springproject.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity 
@Table (name = "process_execution")
@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Process_execution_Entity extends BaseTime{
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long execution_id;

    // fk 설정
    @ManyToOne
    @JoinColumn (name = "batch_id")
    private Batches_Entity batchesEntity;

    @Column(name = "process_code", length = 30)
    private String process_code;

    @Column(name = "start_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime start_time; 

    @Column(name = "end_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime end_time;

    // 실제 소요시간
    @Column(name = "duration_min", precision = 10, scale = 2)
    private BigDecimal duration_min;

    // 공정 진행상태
    @Column(name = "status", length = 30)
    private String status;

    // 데이터 출처
    @Column(name = "record_source", length = 20)
    private String record_source;
    
}
