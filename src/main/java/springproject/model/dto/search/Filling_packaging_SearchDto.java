package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Filling_packaging_SearchDto {
    // 포장·검사 시작일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 포장·검사 종료일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 생산 LOT 번호
    private String batchId;

    // 포장라인
    private String packagingLine;

    // 최종 판정
    private String finalDisposition;

    // 중량검사 결과
    private String checkweigherStatus;

    // 금속검사 결과
    private String metalDetectorStatus;

    // 비전검사 결과
    private String visionInspectionStatus;

    // 담당자 번호
    private Integer userId;
}
