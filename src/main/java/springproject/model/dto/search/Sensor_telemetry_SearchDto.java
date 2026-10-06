package springproject.model.dto.search;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Sensor_telemetry_SearchDto {
    // 측정 시작 일시
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    // 측정 종료 일시
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    // 생산 LOT 번호
    private String batchId;

    // 공정 실행번호
    private Long executionId;

    // 공정코드
    private String processCode;

    // 담당자 번호
    private Integer userId;
    
}
