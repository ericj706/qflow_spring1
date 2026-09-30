package springproject.model.dto;

public interface Chart_Dto {
    String getTimeGroup();  // X축 라벨 (일별, 시간별, LOT별)
    Long getPassCount();    // 양품 생산량
    Long getFailCount();    // 불량 생산량
    Double getDefectRate(); // 불량률 (%)
}
