package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
//등록된 제품의 검사 결과를 수정하는 요청
public class Filling_packaging_Result_Dto {
     // 검사 결과를 입력한 담당자
    private Integer userId;
    // 중량검사 결과
    private String checkweigher_status;
    // 금속검사 결과
    private String metal_detector_status;
    // 비전검사 결과
    private String vision_inspection_status;
    // 최종 판정: 합격 또는 불합격
    private String final_disposition;
    // 비고
    private String note;
}
