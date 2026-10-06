package springproject.model.dto.page;


// 상단 카드의 확인됨·미확인 건수를 DB에서 집계한 결과를 받는 용도
public interface  StatusCount {
    // 조치상태
    // 예: ACKNOWLEDGED, UNACKNOWLEDGED
    String getState();

    // 해당 상태의 알람 건수
    Long getCount();
    
}
