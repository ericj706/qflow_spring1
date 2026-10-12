package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
//최종 검사 승인 요청 DTO
public class Process_execution_QcApproval_Dto {
    // 최종 승인 대상으로 선택한 검사번호
    private String qcId;
    // 승인한 사용자번호
    private Integer userId;
    
}
