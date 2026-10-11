package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
// 심각 알람의 조치내역을 저장
public class Anomaly_event_Action_Dto {
    // 조치한 사용자 번호
    private Integer userId;
    // 실제 수행한 조치내용
    private String actionNote;
}
