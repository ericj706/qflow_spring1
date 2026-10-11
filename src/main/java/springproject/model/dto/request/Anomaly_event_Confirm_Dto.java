package springproject.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
// 알람을 확인한 사용자 번호를 받는 요청 DTO
public class Anomaly_event_Confirm_Dto {
    // 알람을 확인한 사용자 번호
    private Integer userId;
}
