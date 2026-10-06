package springproject.model.dto.search;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Material_dispensing_SearchDto {
    // 칭량기간 시작일
    // Entity의 dispensedAt 기준
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 칭량기간 종료일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 생산 LOT 번호
    private String batchId;

    // 원료코드
    private String materialCode;

    // 원료명:
    private String materialName;

    // 원료 LOT 번호
    private String rawMaterialLot;

    // 칭량상태:
    private String status;

    // 담당자 번호
    private Integer userId;
    
}
