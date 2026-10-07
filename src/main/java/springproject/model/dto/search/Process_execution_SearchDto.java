package springproject.model.dto.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor @Builder @Data 
public class Process_execution_SearchDto {
    // 공정 시작기간 시작일
    // Entity의 start_time 기준
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    // 공정 시작기간 종료일
    // 종료일 당일의 모든 시간을 포함하도록 Service에서 처리
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 생산 LOT 번호: 정확히 일치
    private String batchId;

    // 공정코드: 정확히 일치
    private String processCode;

    // 공정 진행상태: 정확히 일치
    private String status;
    
    // ==========================================
    // 실시간 모니터링/대시보드 전용 DTO 클래스들
    // ==========================================

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessStageStatus {
        private String stageName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String currentSubProcess;
        private String batchId;
        private String note;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RealtimeTelemetry {
        private String batchId;
        private Long executionId;
        private String currentProcessName;
        private BigDecimal tankTempC;
        private BigDecimal phLevel;
        private BigDecimal bulkViscosityCps;
        private BigDecimal paddleRpm;
        private BigDecimal homomixerRpm;
        private BigDecimal motorTorquePct;
        private BigDecimal vacuumKpa;
        private LocalDateTime timestamp;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardResponse {
        private String activeBatchId;
        private List<String> batchList;
        private List<ProcessStageStatus> stages;
        private RealtimeTelemetry latestTelemetry;
        private List<RealtimeTelemetry> telemetryHistory;
    }
    
}
