package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
// 심각 알람의 조치내역을 저장
public class Anomaly_event_Action_Dto {

    // 조치를 수행한 관리자 사원번호
    private Integer userId;

    // 조치 종류 (공정 조건 조정, 설비 점검, 재검사, 생산 일시정지, 기타)
    private String actionType;

    // 관리자가 실제 수행한 조치내용
    private String actionNote;

    // 기타(OTHER) 선택 시 입력하는 비고
    private String actionRemarks;
}
