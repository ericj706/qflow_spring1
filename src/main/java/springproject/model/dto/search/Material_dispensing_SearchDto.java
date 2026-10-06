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
public class Material_dispensing_SearchDto {

    // 칭량일자 시작일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 칭량일자 종료일
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 생산 LOT 번호
    private String batchId;

    // 원료코드
    private String materialCode;

    // 원료명
    private String materialName;

    // 원료 LOT 번호
    private String rawMaterialLot;

    // 칭량상태
    private String status;

    // 담당자 번호
    private Integer userId;
}