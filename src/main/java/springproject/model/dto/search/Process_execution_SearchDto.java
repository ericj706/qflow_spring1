package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Process_execution_SearchDto {
    // 공정 시작기간 시작일
    // Entity의 start_time 기준
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 공정 시작기간 종료일
    // 종료일 당일의 모든 시간을 포함하도록 Service에서 처리
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 생산 LOT 번호: 정확히 일치
    private String batchId;

    // 공정코드: 정확히 일치
    private String processCode;

    // 공정 진행상태: 정확히 일치
    private String status;
    
}
