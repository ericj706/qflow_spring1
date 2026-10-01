package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Anomaly_event_SearchDto {

    // 발생기간 시작일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 발생기간 종료일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 심각도
    private String severity;

    // 조치상태
    private String actionStatus;

    // LOT 번호 검색어: 부분 일치
    private String batchIdKeyword;

    // 공정코드: 정확히 일치
    private String processCode;

    // 이상 유형: 정확히 일치
    private String anomalyType;

    // 조치 담당자 번호
    private Integer userId;
    
}
