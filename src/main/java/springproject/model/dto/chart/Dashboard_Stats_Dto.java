package springproject.model.dto.chart;

public interface Dashboard_Stats_Dto {
    Long getPassCount();            // 최신 LOT 양품 수량
    Long getRejectCount();          // 최신 LOT 불합격 수량
    Long getTotalCount();           // 최신 LOT 전체 수량
    Long getGlobalRejectCount();    // 전체 불합격 수량
    Long getGlobalTotalCount();     // 전체 수량
    Long getAnomalyCount();         // 최신 LOT 이상 발생 건수
}
