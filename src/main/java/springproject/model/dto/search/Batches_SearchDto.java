package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Batches_SearchDto {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // LOT 번호 정확히 일치
    private String batchId;

    // LOT 번호 또는 제품명 부분검색
    private String keyword;

    // 제품코드
    private String productCode;

    // 생산상태
    private String status;

    // 담당자 번호
    private Integer userId;

    // 제조 탱크번호
    private String tankId;
}