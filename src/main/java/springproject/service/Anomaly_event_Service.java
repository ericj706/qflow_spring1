package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.request.Anomaly_event_Action_Dto;
import springproject.model.dto.request.Anomaly_event_Confirm_Dto;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.model.entity.Anomaly_event_Entity;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Anomaly_event_Repository;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Process_execution_Repository;
import springproject.model.repository.Users_Repository;

@Service @RequiredArgsConstructor 
@Transactional (readOnly = true)
public class Anomaly_event_Service {
    private final Anomaly_event_Repository anomaly_event_Repository;

    // 전체조회 + 조건검색 + 페이징
    public Page<Anomaly_event_Dto> search(
            Anomaly_event_SearchDto searchDto,
            int page) {

        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String severity = searchDto.getSeverity();
        String actionStatus = searchDto.getActionStatus();
        String batchId = searchDto.getBatchId();
        String batchIdKeyword = searchDto.getBatchIdKeyword();
        String processCode = searchDto.getProcessCode();
        String anomalyType = searchDto.getAnomalyType();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"종료일의 범위를 확인해 주세요.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // 심각도
        if (severity != null) {
            severity = severity.trim();
            if (severity.isEmpty()) {severity = null;}
        }

        // 조치상태
        if (actionStatus != null) {
            actionStatus = actionStatus.trim();
            if (actionStatus.isEmpty()) {actionStatus = null;}
        }

        // LOT 번호: 정확히 일치하는 검색
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }

        // LOT 번호 검색어: 기존 부분 검색
        if (batchIdKeyword != null) {
            batchIdKeyword = batchIdKeyword.trim();
            if (batchIdKeyword.isEmpty()) {batchIdKeyword = null;}
        }

        // 공정코드
        if (processCode != null) {
            processCode = processCode.trim();
            if (processCode.isEmpty()) {processCode = null;}
        }

        // 이상 유형
        if (anomalyType != null) {
            anomalyType = anomalyType.trim();
            if (anomalyType.isEmpty()) {anomalyType = null;}
        }

        // 시작일 당일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {startAt = startDate.atStartOfDay();}

        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY 사용
        Pageable pageable = PageRequest.of(page, 20);
        Page<Anomaly_event_Entity> result =
                anomaly_event_Repository.search(
                        startAt,
                        endAtExclusive,
                        severity,
                        actionStatus,
                        batchId,
                        batchIdKeyword,
                        processCode,
                        anomalyType,
                        userId,
                        pageable
                );

        // 페이지 정보를 유지하면서 Entity → DTO 변환
        return result.map(Anomaly_event_Dto::from);
    }

    // 1) 개별조회
    public Anomaly_event_Dto findOne( Long anomaly_id){
        Optional<Anomaly_event_Entity> optional = anomaly_event_Repository.findById(anomaly_id);
        if (optional.isPresent()) {
            Anomaly_event_Entity entity = optional.get();
            return Anomaly_event_Dto.from(entity);
        }return null;
    }

    private final Process_execution_Repository process_execution_Repository;

    // 해당 공정 실행에서 발생한 알람 목록 조회
    public List<Anomaly_event_Dto> findByExecutionId(Long executionId){
        // 실행번호 확인
        if(executionId == null || executionId <= 0){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");}
        // 공정 실행 존재 여부 확인
        if (!process_execution_Repository.existsById(executionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행 기록이 없습니다.");}
        // 해당 실행에 연결된 알람 조회
        List<Anomaly_event_Entity> entities = anomaly_event_Repository.findByExecutionId(executionId);
        // Entity → DTO 변환
        List<Anomaly_event_Dto> result = new ArrayList<>();
        for (Anomaly_event_Entity entity : entities) {
            result.add(Anomaly_event_Dto.from(entity));
        }
        return result;
    }

    private final Batches_Repository batches_Repository;
    private final Users_Repository users_Repository;

    // 주의 알람 확인 처리
    @Transactional
    public Anomaly_event_Dto confirm(Long anomalyId, Anomaly_event_Confirm_Dto request) {
        // 1. 알람번호 확인
        if (anomalyId == null || anomalyId <= 0) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 알람번호를 입력해 주세요.");}
        // 2. 사용자번호 확인
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"확인한 사용자 번호를 입력해 주세요.");}
        // 3. 사용자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 사용자가 없습니다.");}
        // 4. 알람에 연결된 공정 실행번호 조회
        Long executionId = anomaly_event_Repository.findExecutionIdByAnomalyId(anomalyId).orElse(null);
        if (executionId == null) {throw new ResponseStatusException(HttpStatus.CONFLICT,"알람이 없거나 공정 실행이 연결되어 있지 않습니다.");}
        // 5. 공정 실행에 연결된 LOT 번호 조회
        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 실행에 연결된 LOT를 확인해 주세요.");}
        // 6. LOT 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");}
        // 7. 공정 실행 잠금
        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행 기록이 없습니다.");}
        // 8. 알람 조회 및 잠금
        Anomaly_event_Entity alarm = anomaly_event_Repository.findByIdForUpdate(anomalyId).orElse(null);
        if (alarm == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 알람이 없습니다.");}
        // 9. 잠금 전에 조회한 공정 실행과 같은지 확인
        if (alarm.getProcess_execution_Entity() == null || !executionId.equals(alarm.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"알람의 공정 연결이 변경되었습니다. 다시 조회해 주세요.");}
        // 10. 주의·심각 알람 확인 처리
        String severity = alarm.getSeverity();
        if (!"주의".equals(severity) && !"심각".equals(severity)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"주의 또는 심각 알람만 확인 처리할 수 있습니다.");
        }
        // 11. 이미 처리했다면 기존 결과 반환
        // 최초 확인자와 처리시간을 덮어쓰지 않음
        if ("확인됨".equals(alarm.getActionStatus()) || "조치완료".equals(alarm.getActionStatus())) {return Anomaly_event_Dto.from(alarm);}
        // 12. 미확인 상태만 변경
        if (!"미확인".equals(alarm.getActionStatus())) {throw new ResponseStatusException(HttpStatus.CONFLICT,"알람의 조치 상태를 확인해 주세요.");}
        // 13. 확인 결과 저장
        alarm.setActionStatus("확인됨");
        alarm.setUsers_Entity(user);
        alarm.setActionTime(LocalDateTime.now());

        Anomaly_event_Entity saved = anomaly_event_Repository.save(alarm);
        return Anomaly_event_Dto.from(saved);
    }

    // 심각 알람 조치 완료 처리
    @Transactional
    public Anomaly_event_Dto completeAction(
            Long anomalyId, Anomaly_event_Action_Dto request) {
        // 1. 알람번호 확인
        if (anomalyId == null || anomalyId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,"올바른 알람번호를 입력해 주세요.");
        }
        // 2. 사용자번호 확인
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"조치한 사용자 번호를 입력해 주세요.");
        }

        // 3. 조치내용 확인
        if (request.getActionNote() == null || request.getActionNote().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,"조치내용을 입력해 주세요.");
        }

        String actionNote = request.getActionNote().strip();

        // 조치 종류는 사전에 정의한 5가지 값만 허용
        String actionType = request.getActionType();

        if (actionType == null ||
                !List.of(
                    "PROCESS_ADJUST",
                    "EQUIPMENT_CHECK",
                    "REINSPECTION",
                    "PRODUCTION_PAUSE",
                    "OTHER"
                ).contains(actionType)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "올바른 조치 종류를 선택해 주세요."
            );
        }

        // 기타(OTHER)를 선택한 경우에만 비고 입력을 필수로 검사
        String actionRemarks = null;

        if ("OTHER".equals(actionType)) {

            if (request.getActionRemarks() == null ||
                    request.getActionRemarks().isBlank()) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기타 조치의 비고를 입력해 주세요."
                );
            }

            actionRemarks = request.getActionRemarks().strip();
        }

        //  조치 유형, 내용, 기타 비고를 하나의 문자열로 구성
        String savedActionNote =
                "[조치유형] " + actionType
                + "\n[조치내용] " + actionNote
                + (actionRemarks != null
                        ? "\n[기타비고] " + actionRemarks
                        : "");

        // 4. 사용자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 사용자가 없습니다.");
        }
        // 5. 알람에 연결된 공정 실행번호 조회
        Long executionId = anomaly_event_Repository.findExecutionIdByAnomalyId(anomalyId).orElse(null);
        if (executionId == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,"알람이 없거나 공정 실행이 연결되어 있지 않습니다.");
        }
        // 6. LOT 번호 조회
        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,"공정 실행에 연결된 LOT를 확인해 주세요.");
        }
        // 7. LOT 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        // 8. 공정 실행 잠금
        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,"해당 공정 실행 기록이 없습니다.");
        }
        // 9. 알람 조회 및 잠금
        Anomaly_event_Entity alarm = anomaly_event_Repository.findByIdForUpdate(anomalyId).orElse(null);
        if (alarm == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,"해당 알람이 없습니다.");
        }
        // 10. 알람의 공정 연결 확인
        if (alarm.getProcess_execution_Entity() == null
                || !executionId.equals(
                        alarm.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,"알람의 공정 연결이 변경되었습니다. 다시 조회해 주세요.");
        }
        // 11. 심각 알람만 처리
        if (!"심각".equals(alarm.getSeverity())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,"심각 알람만 이 기능으로 조치 완료할 수 있습니다.");
        }
        // 12. 이미 조치 완료된 경우
        if ("조치완료".equals(alarm.getActionStatus())) {

            // 같은 요청의 재전송이면 기존 결과 반환
            boolean sameUser = alarm.getUsers_Entity() != null
                    && request.getUserId().equals(
                        alarm.getUsers_Entity().getUserId()
                    );

            // 저장된 조치내역과 현재 요청의 조치내역 비교
            boolean sameNote = savedActionNote.equals(alarm.getActionNote());

            // 같은 담당자가 동일한 조치내역을 다시 전송했다면 기존 결과 반환
            if (sameUser && sameNote) {
                return Anomaly_event_Dto.from(alarm);
            }

            // 기존 조치 기록을 새 요청으로 덮어쓰지 않음
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 조치 완료된 알람입니다. 기존 조치내역을 확인해 주세요."
            );
}

        // 13. 확인을 먼저 완료한 심각 알림만 조치 완료 가능
        String actionStatus = alarm.getActionStatus();
        if(!"확인됨".equals(actionStatus)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"심각 알람을 먼저 확인한 뒤 조치 완료해 주세요.");
        }

        // 15. 기존 action_note 컬럼에 조치내역 저장
        alarm.setActionNote(savedActionNote);

        // 16. 조치 상태, 담당자, 처리시간 저장
        alarm.setActionStatus("조치완료");
        alarm.setUsers_Entity(user);
        alarm.setActionTime(LocalDateTime.now());

        // 17. 변경된 알람 정보 저장
        Anomaly_event_Entity saved = anomaly_event_Repository.save(alarm);

        // 18. 저장된 결과를 DTO로 변환하여 반환
        return Anomaly_event_Dto.from(saved);
    }

    
}
