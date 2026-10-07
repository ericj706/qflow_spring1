package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.entity.Material_dispensing_Entity;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Sensor_telemetry_Entity;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Bulk_qc_Repository;
import springproject.model.repository.Filling_packaging_Repository;
import springproject.model.repository.Material_dispensing_Repository;
import springproject.model.repository.Process_execution_Repository;
import springproject.model.repository.Sensor_telemetry_Repository;

@Service
@RequiredArgsConstructor
public class Process_execution_Service {

    private final Process_execution_Repository pr;
    private final Batches_Repository batchesRepository;
    private final Material_dispensing_Repository dispensingRepository;
    private final Bulk_qc_Repository bulkQcRepository;
    private final Filling_packaging_Repository packagingRepository;
    private final Sensor_telemetry_Repository telemetryRepository;

    // 1. 전체조회 + 조건검색 + 페이징
    public Page<Process_execution_Dto> search(
            Process_execution_SearchDto searchDto,
            int page
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일 범위를 확인해 주세요.");
        }

        String batchId = searchDto.getBatchId();
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) { batchId = null; }
        }

        String processCode = searchDto.getProcessCode();
        if (processCode != null) {
            processCode = processCode.trim();
            if (processCode.isEmpty()) { processCode = null; }
        }

        String status = searchDto.getStatus();
        if (status != null) {
            status = status.trim();
            if (status.isEmpty()) { status = null; }
        }

        LocalDateTime startAt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endAtExclusive = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(page, 20);

        Page<Process_execution_Entity> entities = pr.search(
                startAt,
                endAtExclusive,
                batchId,
                processCode,
                status,
                pageable
        );

        return entities.map(Process_execution_Dto::from);
    }

    // 2. 개별조회 (PK 기준)
    public Process_execution_Dto findOne(Long execution_id) {
        Process_execution_Entity entity = pr.findById(execution_id).orElse(null);
        if (entity == null) {
            return null;
        }
        return Process_execution_Dto.from(entity);
    }

    // 3. 실시간 공정 모니터링 대시보드 데이터 조회
    public Process_execution_Dto.DashboardResponse getDashboardData(String selectedBatchId) {
        // (1) 전체 LOT 목록 조회 및 Target Batch 선택
        List<Batches_Entity> allBatches = batchesRepository.findAllByOrderByStartTimeDesc();
        if (allBatches == null || allBatches.isEmpty()) return null;

        String targetBatchId = (selectedBatchId != null && !selectedBatchId.trim().isEmpty()) 
                ? selectedBatchId.trim() 
                : allBatches.get(0).getBatchId();

        // (2) 해당 LOT의 공정별 데이터 조회
        List<Material_dispensing_Entity> dispensingList = dispensingRepository.findByBatchId(targetBatchId);
        List<Process_execution_Entity> executionList = pr.findByBatchesEntity(targetBatchId);
        if (executionList == null) executionList = Collections.emptyList();

        List<Bulk_qc_Entity> bulkQcList = bulkQcRepository.findByBatchId(targetBatchId);
        List<Filling_packaging_Entity> packagingList = packagingRepository.findByBatchId(targetBatchId);

        // (3) 공정 세부 존재 여부 판별
        boolean hasDispensing = (dispensingList != null && !dispensingList.isEmpty());

        Process_execution_Entity heatMixExec = executionList.stream()
                .filter(e -> "가열/혼합".equals(e.getProcess_code()) || "HEAT_MIX".equals(e.getProcess_code()))
                .findFirst().orElse(null);

        Process_execution_Entity coolExec = executionList.stream()
                .filter(e -> "냉각".equals(e.getProcess_code()) || "COOLING".equals(e.getProcess_code()))
                .findFirst().orElse(null);

        boolean hasHeatMix = (heatMixExec != null);
        boolean hasCooling = (coolExec != null);
        boolean hasBulkQc = (bulkQcList != null && !bulkQcList.isEmpty());
        boolean hasPackaging = (packagingList != null && !packagingList.isEmpty());

        // (4) 5단계 공정 상태 구성
        List<Process_execution_Dto.ProcessStageStatus> stages = new ArrayList<>();

        // Stage 1: 원료 칭량
        stages.add(Process_execution_Dto.ProcessStageStatus.builder()
                .stageName("원료 칭량")
                .batchId(targetBatchId)
                .status(hasDispensing ? (hasHeatMix ? "완료" : "진행중") : "대기")
                .startTime(hasDispensing ? dispensingList.get(0).getDispensedAt() : null)
                .endTime(hasHeatMix ? heatMixExec.getStart_time() : null)
                .build());

        // Stage 2: 가열·혼합
        stages.add(Process_execution_Dto.ProcessStageStatus.builder()
                .stageName("가열·혼합")
                .batchId(targetBatchId)
                .status(hasHeatMix ? (hasCooling ? "완료" : "진행중") : "대기")
                .startTime(hasHeatMix ? heatMixExec.getStart_time() : null)
                .endTime(hasHeatMix ? heatMixExec.getEnd_time() : null)
                .currentSubProcess("가열/혼합")
                .build());

        // Stage 3: 냉각·마무리
        stages.add(Process_execution_Dto.ProcessStageStatus.builder()
                .stageName("냉각·마무리")
                .batchId(targetBatchId)
                .status(hasCooling ? (hasBulkQc ? "완료" : "진행중") : "대기")
                .startTime(hasCooling ? coolExec.getStart_time() : null)
                .endTime(hasCooling ? coolExec.getEnd_time() : null)
                .currentSubProcess("냉각")
                .build());

        // Stage 4: 벌크 QC
        stages.add(Process_execution_Dto.ProcessStageStatus.builder()
                .stageName("벌크 QC")
                .batchId(targetBatchId)
                .status(hasBulkQc ? (hasPackaging ? "완료" : "진행중") : "대기")
                .startTime(hasBulkQc ? bulkQcList.get(0).getSample_time() : null)
                .build());

        // Stage 5: 충진·포장
        stages.add(Process_execution_Dto.ProcessStageStatus.builder()
                .stageName("충진·포장")
                .batchId(targetBatchId)
                .status(hasPackaging ? "완료" : (hasBulkQc ? "진행중" : "대기"))
                .startTime(hasPackaging ? packagingList.get(0).getTimestamp() : null)
                .build());

        // (5) 센서 Telemetry 매핑
        List<Long> execIds = executionList.stream()
                .map(Process_execution_Entity::getExecution_id)
                .collect(Collectors.toList());

        List<Sensor_telemetry_Entity> telemetryList = execIds.isEmpty() 
                ? Collections.emptyList() 
                : telemetryRepository.findByExecutionIdInOrderByTimestampAsc(execIds);

        Process_execution_Dto.RealtimeTelemetry latestTelemetry = null;
        if (telemetryList != null && !telemetryList.isEmpty()) {
            Sensor_telemetry_Entity latest = telemetryList.get(telemetryList.size() - 1);
            String currentProcess = (coolExec != null && latest.getExecution_id().equals(coolExec.getExecution_id())) 
                    ? "냉각" 
                    : "가열/혼합";

            latestTelemetry = Process_execution_Dto.RealtimeTelemetry.builder()
                    .batchId(targetBatchId)
                    .executionId(latest.getExecution_id())
                    .currentProcessName(currentProcess)
                    .tankTempC(latest.getTank_temp_c())
                    .phLevel(latest.getPh_level())
                    .bulkViscosityCps(latest.getBulk_viscosity_cps())
                    .paddleRpm(latest.getPaddle_rpm())
                    .homomixerRpm(latest.getHomomixer_rpm())
                    .motorTorquePct(latest.getMotor_torque_pct())
                    .vacuumKpa(latest.getVacuumKpa())
                    .timestamp(latest.getTimestamp())
                    .build();
        }

        List<Process_execution_Dto.RealtimeTelemetry> history = (telemetryList == null) 
                ? Collections.emptyList() 
                : telemetryList.stream().map(t -> {
                    String processName = (coolExec != null && t.getExecutionId().equals(coolExec.getExecution_id())) 
                            ? "냉각" 
                            : "가열/혼합";
                    return Process_execution_Dto.RealtimeTelemetry.builder()
                            .executionId(t.getExecutionId())
                            .currentProcessName(processName)
                            .tankTempC(t.getTankTempC())
                            .phLevel(t.getPhLevel())
                            .bulkViscosityCps(t.getBulkViscosityCps())
                            .paddleRpm(t.getPaddleRpm())
                            .homomixerRpm(t.getHomomixerRpm())
                            .motorTorquePct(t.getMotorTorquePct())
                            .vacuumKpa(t.getVacuumKpa())
                            .timestamp(t.getTimestamp())
                            .build();
                }).collect(Collectors.toList());

        List<String> batchIds = allBatches.stream()
                .map(Batches_Entity::getBatchId)
                .collect(Collectors.toList());

        return Process_execution_Dto.DashboardResponse.builder()
                .activeBatchId(targetBatchId)
                .batchList(batchIds)
                .stages(stages)
                .latestTelemetry(latestTelemetry)
                .telemetryHistory(history)
                .build();
    }
}