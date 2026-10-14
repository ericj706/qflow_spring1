package springproject.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.Process_execution_Finish_Dto;
import springproject.model.dto.request.Process_execution_QcApproval_Dto;
import springproject.model.dto.request.Process_execution_Start_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.model.entity.Anomaly_event_Entity;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Anomaly_event_Repository;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Bulk_qc_Repository;
import springproject.model.repository.Filling_packaging_Repository;
import springproject.model.repository.Filling_packaging_Repository.ExecutionPackagingSummary;
import springproject.model.repository.Process_execution_Repository;
import springproject.model.repository.Users_Repository;


@Service
@RequiredArgsConstructor

public class Process_execution_Service {
    private final Process_execution_Repository pr;
    private final Batches_Repository batches_Repository;

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
 
    // 공정 등록 ( 새 LOT의 첫 공정 시작 )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Process_execution_Dto save(Process_execution_Start_Dto request){
        // 1. LOT 번호 확인
        if (request == null || request.getBatchId() == null || request.getBatchId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"LOT 번호를 입력해 주세요.");
        }
        // 2. 공정명 확인
        if (request.getProcessCode() == null || request.getProcessCode().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"시작할 공정을 입력해 주세요.");
        }
        String batchId = request.getBatchId().trim();
        String processCode = request.getProcessCode().trim();
        // 3. 직접 시작은 첫 공정만 허용
        if (!"원료 칭량".equals(processCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,"첫 공정인 원료 칭량부터 시작해 주세요. "+ "이후 공정은 이전 공정의 진행 조건을 확인한 뒤 시작됩니다.");
        }
        // 4. LOT 조회 및 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        // 5. 이미 실행 이력이 있는 LOT는 다시 시작할 수 없음
        boolean hasExecution = pr.existsByBatchesEntity_BatchId(batchId);
        if (hasExecution) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 공정 실행 이력이 있는 LOT입니다. "+ "기존 실행의 종료 또는 다음 공정 진행 기능을 사용해 주세요.");
        }

        // 6. 대기 상태인 LOT만 처음 시작할 수 있음
        if (!"대기".equals(batch.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"대기 상태인 LOT만 공정을 시작할 수 있습니다.");
        }

        LocalDateTime startTime = LocalDateTime.now();
        // 첫 공정의 담당관리자 조회
        Users_Entity managerUser = findProcessManager(processCode);

        // 첫 공정 실행 기록 생성
        Process_execution_Entity entity = Process_execution_Entity.builder()
                                            .batchesEntity(batch)
                                            .managerUser(managerUser)
                                            .process_code(processCode)
                                            .start_time(startTime)
                                            .status("진행중")
                                            .record_source("수동입력")
                                            .build();

        // 8. LOT도 생산 진행 상태로 변경
        batch.setStatus("진행중");
        batch.setStartTime(startTime);

        // 조회한 batch는 트랜잭션 안에서 변경 내용이 자동 반영됨
        Process_execution_Entity saved = pr.save(entity);
        Process_execution_Dto result = Process_execution_Dto.from(saved);
        return result;

} 

    // 공정 종료  )) 조회 -> 상태를 변경 후 소요시간과 함께 저장
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Process_execution_Finish_Dto finish(Long executionId){
        // 1.실행 번호 확인
        if(executionId == null || executionId <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");}
        // 2. 해당 실행의 LOT 번호 조회
        String batchId = pr.findBatchIdByExecutionId(executionId).orElse(null);
        if( batchId == null ){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT 없음.");}
        
        // 3. LOT 잠금 중복 처리 안되게끔
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if(batch == null){ throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");}

        // 4. 공정 실행 기록 조회 및 잠금
        Process_execution_Entity entity = pr.findByIdForUpdate(executionId).orElse(null);
        if (entity == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행 기록이 없습니다.");}

        // 5. 진행 중인 공정만 종료 가능
        if( !"진행중".equals(entity.getStatus())){throw new ResponseStatusException(HttpStatus.CONFLICT,"진행 중인 공정만 종료할 수 있습니다.");}
        // 최종 포장 검사는 모든 제품의 판정이 끝나야 종료 가능
        boolean finalPackaging = "최종 포장 검사".equals(entity.getProcess_code());
        ExecutionPackagingSummary packagingSummary = null;
        if (finalPackaging) {packagingSummary = checkPackagingResult(executionId);}

        // 6. 시작시간 확인 
        LocalDateTime startTime = entity.getStart_time();
        LocalDateTime endTime = LocalDateTime.now();
        if(startTime == null || startTime.isAfter(endTime)){throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 시작시간을 확인해 주세요.");}
        // 7. 소요시간 분 단위 계산
        Long elapsedMillis = Duration.between(startTime, endTime).toMillis();
        BigDecimal duration_Min = BigDecimal.valueOf(elapsedMillis).divide(BigDecimal.valueOf(60_000),2,RoundingMode.HALF_UP);

        // 8. 다음 공정 명 확인 ( 일단 벌크검사까지는 자동적으로 넘어갈 예쩡)
        String nextProcessCode = getNextProcessCode(entity.getProcess_code());

        // 9. 알람 처리 여부와 벌크 검사 승인 결과 확인
        boolean alarmsHandled = checkAlarmStatus(entity);
        String qcBlockReason = checkBulkQcStatus(entity);
        // 다음 공정이 있고, 알람과 검사 조건을 모두 충족하면 자동 시작
        boolean startNext = nextProcessCode != null && alarmsHandled && qcBlockReason == null;
        
        // . 자동 시작할 공정이 이미 진행 중인지 확인
        if(startNext){
            long nextCount = pr.countByBatchAndProcessAndStatus(batchId, nextProcessCode, "진행중");
            if(nextCount > 0){throw new ResponseStatusException(HttpStatus.CONFLICT,"다음 공정이 이미 진행 중입니다. 실행 이력을 확인해 주세요.");}
        }

        // 10. 현재 공정 종료
        entity.setEnd_time(endTime);
        entity.setDuration_min(duration_Min);
        entity.setStatus("완료");
        Process_execution_Entity finished = pr.save(entity);

        // 최종 공정이며 알람 처리까지 끝났다면 LOT도 완료
        if (finalPackaging && alarmsHandled) {completeBatch(batch, finished, packagingSummary);}

        // 다음 공정 자동 시작
        Process_execution_Dto nextExecution = null;
        if (startNext) {
            // 다음 공정의 담당관리자 조회
            Users_Entity managerUser = findProcessManager(nextProcessCode);
            Process_execution_Entity nextEntity = Process_execution_Entity.builder()
                                                .batchesEntity(batch)
                                                .managerUser(managerUser)
                                                .process_code(nextProcessCode)
                                                .start_time(endTime)
                                                .status("진행중")
                                                .record_source("자동진행")
                                                .previousExecution(finished)
                                                .build();
            Process_execution_Entity nextSaved = pr.save(nextEntity);
            nextExecution = Process_execution_Dto.from(nextSaved);
        }

        // 13. 다음 공정 진행 상태와 안내 메시지 결정
        String nextProcessStatus; String message;
        if (!alarmsHandled) {
            nextProcessStatus = "알람 처리 대기";
            boolean earlyProcess = "원료 칭량".equals(entity.getProcess_code()) || "가열/혼합".equals(entity.getProcess_code()) || "냉각".equals(entity.getProcess_code());
            if (earlyProcess) {
                message = "심각 알람의 확인과 조치를 완료해 주세요. " + "알람의 심각도 정보도 확인해 주세요.";
            } else { message = "주의 알람은 확인하고, 심각 알람은 확인과 조치를 완료해 주세요. " + "알람의 심각도 정보도 확인해 주세요.";}
        } else if (qcBlockReason != null) {
            nextProcessStatus = "검사 확인 대기";
            message = qcBlockReason;
        } else if (nextExecution != null) {
            nextProcessStatus = "다음 공정 시작";
            message = nextProcessCode + " 공정이 시작되었습니다.";
        } else if (finalPackaging) {
            nextProcessStatus = "LOT 완료";
            message = "최종 포장 검사와 LOT 생산이 완료되었습니다.";
        } else {
            nextProcessStatus = "마지막 공정 종료";
            message = "공정이 종료되었습니다.";
        }

        // 14. 종료 결과 반환
        return Process_execution_Finish_Dto.builder()
                .finishedExecution(Process_execution_Dto.from(finished))
                .nextExecution(nextExecution)
                .nextProcessStatus(nextProcessStatus)
                .message(message)
                .build();
    }

    // 현재 공정에 이어서 진행할 공정으로 공정명 변경 (순서대로 공정 실행)
    public String getNextProcessCode(String processCode){
        if(processCode == null){throw new ResponseStatusException(HttpStatus.CONFLICT,"현재 공정명이 없습니다.");}
        return switch(processCode){
            case "원료 칭량"->"가열/혼합";
            case "가열/혼합"->"냉각";
            case "냉각"->"벌크 검사";
            case "벌크 검사"->"최종 포장 검사";
            case "최종 포장 검사"->null;
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT,"확인할 수 없는 공정임");
        };
    }

    private final Anomaly_event_Repository anomaly_event_Repository;
    // 현재 공정의 다음 단계 진행을 막는 알람이 있는지 확인
    private boolean checkAlarmStatus(Process_execution_Entity execution) {
        if (execution == null || execution.getExecution_id() == null) {return false;}
        String processCode = execution.getProcess_code();
        if (processCode == null) {return false;}

        // 1~3공정은 주의 알람이 미확인이어도 진행 허용
        // 벌크 검사와 최종 포장 검사는 기존 주의 확인 조건 유지
        boolean allowUnconfirmedWarning;
        switch (processCode) {
            case "원료 칭량":
            case "가열/혼합":
            case "냉각":
                allowUnconfirmedWarning = true;
                break;
            case "벌크 검사":
            case "최종 포장 검사":
                allowUnconfirmedWarning = false;
                break;
            default:
                // 알 수 없는 공정이면 진행 보류
                return false;
        }

        List<Anomaly_event_Entity> alarms = anomaly_event_Repository.findByExecutionId(execution.getExecution_id());
        for (Anomaly_event_Entity alarm : alarms) {
            String severity = alarm.getSeverity();
            String actionStatus = alarm.getActionStatus();
            // 정상은 진행 허용
            if ("정상".equals(severity)) {continue;}
            // 주의 알람
            if ("주의".equals(severity)) {
                // 1~3공정: 알람 상태는 변경하지 않고 진행 허용
                if (allowUnconfirmedWarning) {continue;}
                // 4~5공정: 확인 또는 조치 완료 필요
                boolean handled = "확인됨".equals(actionStatus) || "조치완료".equals(actionStatus);
                if (!handled) {return false;}
                continue;
            }

            // 모든 공정의 심각 알람은 조치 완료 필요
            // 확인 → 조치 완료 순서는 알람 서비스에서 검증
            if ("심각".equals(severity)) {
                String actionNote = alarm.getActionNote();
                boolean handled = "조치완료".equals(actionStatus) && actionNote != null && !actionNote.isBlank();
                if (!handled) {return false;}
                continue;
            }
           // 심각도가 없거나 알 수 없는 값이면 진행 보류
            return false;
        }
        return true;
    }

    // 완료된 공정의 진행 조건을 재검사하고 다음 공정 시작
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Process_execution_Finish_Dto resume(Long executionId) {
        // 1. 실행번호 확인
        if (executionId == null || executionId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");
        }
        // 2. LOT 번호 조회
        String batchId = pr.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }
        // 3. LOT 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        // 4. 완료된 공정 실행 조회 및 잠금
        Process_execution_Entity entity = pr.findByIdForUpdate(executionId).orElse(null);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행 기록이 없습니다.");
        }
        // 5. 완료된 실행에서만 다음 공정 진행 가능
        if (!"완료".equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"현재 공정을 먼저 종료해 주세요.");
        }
        // 6. 이미 다음 공정이 생성됐다면 기존 결과 반환
        Process_execution_Entity existingNext = pr.findNextByExecutionId(executionId).orElse(null);
        if (existingNext != null) {
            return Process_execution_Finish_Dto.builder()
                    .finishedExecution(Process_execution_Dto.from(entity))
                    .nextExecution(Process_execution_Dto.from(existingNext))
                    .nextProcessStatus("다음 공정 생성됨")
                    .message("이미 생성된 다음 공정의 실행 정보를 반환합니다.")
                    .build();
        }
        // 7. 다음 공정명 및 알람 처리 여부 확인
        String nextProcessCode = getNextProcessCode(entity.getProcess_code());
        boolean alarmsHandled = checkAlarmStatus(entity);
        String qcBlockReason = checkBulkQcStatus(entity);
        Process_execution_Dto nextExecution = null;
        
        String nextProcessStatus;
        String message;
        if (!alarmsHandled) {
            nextProcessStatus = "알람 처리 대기";
            boolean earlyProcess =
                    "원료 칭량".equals(entity.getProcess_code())
                    || "가열/혼합".equals(entity.getProcess_code())
                    || "냉각".equals(entity.getProcess_code());
            if (earlyProcess) {
                message = "심각 알람의 확인과 조치를 완료해 주세요. "+ "알람의 심각도 정보도 확인해 주세요.";
            } else {
                message = "주의 알람은 확인하고, 심각 알람은 확인과 조치를 완료해 주세요. "+ "알람의 심각도 정보도 확인해 주세요.";
            }
        } else if (qcBlockReason != null) {
            nextProcessStatus = "검사 확인 대기";
            message = qcBlockReason;
        } else if (nextProcessCode == null) {
            // 마지막 공정: 이미 완료된 LOT라면 기존 완료 상태 유지
            if (!"완료".equals(batch.getStatus())) {
                ExecutionPackagingSummary packagingSummary = checkPackagingResult(executionId);
                completeBatch(batch, entity, packagingSummary);
            }
            nextProcessStatus = "LOT 완료";
            message = "최종 포장 검사와 LOT 생산이 완료되었습니다.";
        } else {
            // 같은 LOT의 다음 공정이 이미 진행 중인지 확인
            long nextCount = pr.countByBatchAndProcessAndStatus(batchId,nextProcessCode,"진행중");
            if (nextCount > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"같은 LOT의 다음 공정이 이미 진행 중입니다. 실행 이력을 확인해 주세요.");
            }

            // 다음 공정의 담당관리자 조회
        Users_Entity managerUser = findProcessManager(nextProcessCode);
        // 다음 공정 실행 기록 생성
        Process_execution_Entity nextEntity = Process_execution_Entity.builder()
                                                .batchesEntity(batch)
                                                .managerUser(managerUser)
                                                .previousExecution(entity)
                                                .process_code(nextProcessCode)
                                                .start_time(LocalDateTime.now())
                                                .status("진행중")
                                                .record_source("자동진행")
                                                .build();

        Process_execution_Entity nextSaved = pr.save(nextEntity);
        nextExecution = Process_execution_Dto.from(nextSaved);

        nextProcessStatus = "다음 공정 시작";
        message = nextProcessCode + " 공정이 시작되었습니다.";message = nextProcessCode + " 공정이 시작되었습니다.";}
        
        // 10. 진행 재검사 결과 반환
        return Process_execution_Finish_Dto.builder()
                .finishedExecution(Process_execution_Dto.from(entity))
                .nextExecution(nextExecution)
                .nextProcessStatus(nextProcessStatus)
                .message(message)
                .build();
    }

    private final Bulk_qc_Repository bulk_qc_Repository;
    private final Users_Repository users_Repository;

    // 벌크 검사의 최종 결과 승인
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Process_execution_Dto approveBulkQc(
            Long executionId,
            Process_execution_QcApproval_Dto request) {
        // 1. 실행번호와 요청값 확인
        if (executionId == null || executionId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");
        }
        if (request == null || request.getQcId() == null || request.getQcId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"최종 승인할 검사번호를 입력해 주세요.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"승인한 사용자 번호를 입력해 주세요.");
        }

        String qcId = request.getQcId().trim();
        // 2. LOT 번호 조회
        String batchId = pr.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }
        // 3. LOT 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        // 4. 공정 실행 조회 및 잠금
        Process_execution_Entity execution = pr.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행이 없습니다.");
        }
        // 5. 벌크 검사 공정인지 확인
        if (!"벌크 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"벌크 검사 공정에서만 최종 검사 결과를 승인할 수 있습니다.");
        }
        if (!"진행중".equals(execution.getStatus()) && !"완료".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"현재 공정 상태에서는 검사 결과를 승인할 수 없습니다.");
        }
        // 6. 승인 사용자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 사용자가 없습니다.");
        }
        // 7. 검사 결과 조회
        Bulk_qc_Entity qc = bulk_qc_Repository.findById(qcId).orElse(null);
        if (qc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 검사 결과가 없습니다.");
        }
        // 8. 같은 공정 실행에 속한 검사인지 확인
        if (qc.getProcess_execution_Entity() == null
                || !executionId.equals(qc.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"선택한 검사 결과가 해당 공정 실행에 속하지 않습니다.");
        }
        // 9. 같은 LOT인지 확인
        if (qc.getBatchesEntity() == null || !batchId.equals(qc.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"검사 결과와 공정 실행의 LOT가 다릅니다.");
        }
        // 10. 판정이 확정된 결과인지 확인
        String result = qc.getOverall_qc_result();
        if (!"합격".equals(result) && !"불합격".equals(result)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"검사 판정이 합격 또는 불합격으로 확정되어야 합니다.");
        }
        // 11. 이미 같은 검사를 승인했다면 기존 승인 정보 유지
        if (execution.getFinalBulkQc() != null
                && qcId.equals(execution.getFinalBulkQc().getQc_id())
                && execution.getQcApprovedAt() != null
                && execution.getQcApprovedBy() != null) {
            return Process_execution_Dto.from(execution);
        }
        // 12. 다음 공정이 생성된 후에는 승인 대상 변경 금지
        Process_execution_Entity nextExecution = pr.findNextByExecutionId(executionId).orElse(null);
        if (nextExecution != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"다음 공정이 이미 시작되어 승인 대상을 변경할 수 없습니다.");
        }
        // 13. 최종 승인 정보 저장
        execution.setFinalBulkQc(qc);
        execution.setQcApprovedBy(user);
        execution.setQcApprovedAt(LocalDateTime.now());
        Process_execution_Entity saved = pr.save(execution);

        return Process_execution_Dto.from(saved);
    }

    // 벌크 검사 이후 다음 공정으로 진행할 수 있는지 확인
    // null 반환: 진행 가능
    // 문자열 반환: 진행할 수 없는 이유
    private String checkBulkQcStatus(Process_execution_Entity execution) {
        // 벌크 검사 이외의 공정에는 이 조건을 적용하지 않음
        if (!"벌크 검사".equals(execution.getProcess_code())) {return null;}
        
        Bulk_qc_Entity qc = execution.getFinalBulkQc();
        // 최종 승인한 검사와 승인 정보 확인
        if (qc == null || execution.getQcApprovedBy() == null || execution.getQcApprovedAt() == null) {
            return "최종 검사 결과를 승인해 주세요.";
        }
        // 승인된 검사가 현재 공정 실행에 속하는지 확인
        if (qc.getProcess_execution_Entity() == null || !execution.getExecution_id().equals(qc.getProcess_execution_Entity().getExecution_id())) {
            return "최종 승인된 검사의 공정 실행번호를 확인해 주세요.";
        }
        // 승인된 검사와 현재 공정의 LOT가 같은지 확인
        if (execution.getBatchesEntity() == null
                || qc.getBatchesEntity() == null
                || !execution.getBatchesEntity().getBatchId().equals(
                        qc.getBatchesEntity().getBatchId())) {
            return "최종 승인된 검사의 LOT 번호를 확인해 주세요.";
        }
        // 최종 승인된 한 건의 판정을 기준으로 판단
        if ("합격".equals(qc.getOverall_qc_result())) {
            return null;
        }
        if ("불합격".equals(qc.getOverall_qc_result())) {
            return "최종 승인된 벌크 검사가 불합격입니다. 다음 공정을 진행할 수 없습니다.";
        }
        return "최종 승인된 벌크 검사의 판정값을 확인해 주세요.";
    }

    private final Filling_packaging_Repository filling_packaging_Repository;

    // 해당 공정 실행에 등록된 모든 제품의 판정이 끝났는지 확인
    private ExecutionPackagingSummary checkPackagingResult(Long executionId) {
        ExecutionPackagingSummary summary = filling_packaging_Repository.findSummaryByExecutionId(executionId);
        long totalCount = summary.getTotalCount();
        long passCount = summary.getPassCount();
        long failCount = summary.getFailCount();
        if (totalCount == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"등록된 포장·검사 제품이 없습니다.");
        }
        long pendingCount = totalCount - passCount - failCount;
        if (pendingCount > 0) {throw new ResponseStatusException(HttpStatus.CONFLICT,"판정이 완료되지 않은 제품이 "+ pendingCount + "개 있습니다.");
        }
        // LOT의 수량 필드가 Integer이므로 저장 범위 확인
        if (passCount > Integer.MAX_VALUE|| failCount > Integer.MAX_VALUE) {throw new ResponseStatusException(HttpStatus.CONFLICT,"제품 수량이 LOT 수량 필드의 저장 범위를 초과했습니다.");
        }
        return summary;
    }

    // 최종 포장 공정 종료 후 LOT 완료 처리  // actualUnits는 전체 생산수량이 아니라 양품수량으로 정의
    // 호출 전에 알람 처리 여부와 포장 판정 완료 여부를 확인해야 함
    private void completeBatch(Batches_Entity batch,Process_execution_Entity execution,ExecutionPackagingSummary summary) {
        if (!"최종 포장 검사".equals(execution.getProcess_code()) || !"완료".equals(execution.getStatus()) || execution.getEnd_time() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 포장 검사 공정이 종료되어야 LOT를 완료할 수 있습니다.");
        }
        // 반복 요청으로 완료시간이나 수량을 다시 변경하지 않음
        if ("완료".equals(batch.getStatus())) {return;}
        if (!"진행중".equals(batch.getStatus())) {throw new ResponseStatusException(HttpStatus.CONFLICT,"진행중 상태의 LOT만 완료할 수 있습니다.");
        }
        batch.setActualUnits(Math.toIntExact(summary.getPassCount()));
        batch.setDefectUnits(Math.toIntExact(summary.getFailCount()));

        // 알람 확인 시각이 아닌 실제 마지막 공정 종료시각 사용
        batch.setEndTime(execution.getEnd_time());
        batch.setStatus("완료");

        // finish()/resume()의 트랜잭션 안에서 변경 내용이 자동 저장됨
    }

    // 해당 공정의 활성 관리자 중 담당 공정이 없는 첫 번째 관리자 조회
    private Users_Entity findProcessManager(String processCode) {
        if (processCode == null || processCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당관리자를 배정할 공정명이 없습니다.");
        }
        String role = switch (processCode) {
            case "원료 칭량" -> "1공정관리자";
            case "가열/혼합" -> "2공정관리자";
            case "냉각" -> "3공정관리자";
            case "벌크 검사" -> "4공정관리자";
            case "최종 포장 검사" -> "5공정관리자";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"지원하지 않는 공정입니다: " + processCode);
        };
        // 사용자번호가 작은 순서로 조회
        List<Users_Entity> managers = users_Repository.findManagersForUpdate(role);
        if (managers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,processCode + " 공정의 활성 담당관리자가 없습니다.");
        }
        // 담당 중인 공정이 없는 관리자를 순서대로 찾음
        for (Users_Entity manager : managers) {
            long assignedCount = pr.countAssignedProcesses(manager.getUserId());
            if (assignedCount == 0) {return manager;}
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,processCode + " 공정의 관리자가 모두 다른 공정을 담당 중입니다.");
    }


}
