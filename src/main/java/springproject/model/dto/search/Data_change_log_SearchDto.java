package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Data_change_log_SearchDto {
    // 변경기간 시작일: changed_at 기준
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 변경기간 종료일: changed_at 기준
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 변경 대상 테이블명: 정확히 일치
    private String tableName;

    // 변경 대상 레코드 번호: 정확히 일치
    private String recordId;

    // 변경 대상 레코드 번호 검색어: 부분 일치
    private String recordIdKeyword;

    // 변경 컬럼명: 정확히 일치
    private String columnName;

    // 변경 유형: INSERT, UPDATE, DELETE
    private String changeType;

    // 변경 작업자 번호
    private Integer userId;
    
}
