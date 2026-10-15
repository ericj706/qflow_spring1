package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import jakarta.servlet.http.HttpSession;
import springproject.model.dto.Completed_action_notification_Dto;

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
import springproject.model.entity.Completed_action_notification_Entity;
import springproject.model.repository.Completed_action_notification_Repository;

@Service @RequiredArgsConstructor 

@Transactional (readOnly = true)

public class Anomaly_event_Service {

    private final Anomaly_event_Repository anomaly_event_Repository;
    private final Completed_action_notification_Repository completed_action_notification_Repository;

    // 전체조회 + 조건검색 + 페이징
    public Page<Anomaly_event_Dto> search(
            Anomaly_event_SearchDto searchDto,
            int page,
            HttpSession session) {

    // 로그인한 관리자 확인
    Users_Entity loginUser = requireLoginManager(session);

    // 최고관리자인지 확인
    boolean isSuperAdmin =
            "최고관리자".equals(loginUser.getRole());

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
        // 최고관리자: 전체 공정 조회 가능
        // 공정관리자: 본인 담당 공정만 조회 가능
        if (!isSuperAdmin) {

            String managerProcessCode =
                    processCodeForRole(loginUser.getRole());

            // 다른 공정을 검색하려는 경우 차단
            if (processCode != null &&
                    !processCode.equals(managerProcessCode)) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "담당 공정의 이상내역만 조회할 수 있습니다."
                );
            }

            // 검색조건과 관계없이 담당 공정으로 제한
            processCode = managerProcessCode;
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
        String sourceType = searchDto.getSourceType();

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY 사용
        Pageable pageable = PageRequest.of(page, 20);
        Page<Anomaly_event_Entity> result =

            anomaly_event_Repository.search(

                    startAt,
                    endAtExclusive,
                    severity,
                    actionStatus,
                    sourceType,       // 추가
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

    // =============================================================
    // 로그인 사용자 검증 및 담당 공정 판별
    // 주의: React localStorage의 role은 신뢰하지 않고 서버 세션 + DB를 확인한다.
    // =============================================================
    private Users_Entity requireLoginManager(HttpSession session) {
        Integer loginUserId = (Integer) session.getAttribute("LOGIN_USER_ID");
        if (loginUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Users_Entity loginUser = users_Repository.findById(loginUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 계정을 찾을 수 없습니다."));
        if (!Boolean.TRUE.equals(loginUser.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비활성화된 계정입니다.");
        }
        if (!isManagerRole(loginUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return loginUser;
    }

    // 현재 시스템에 정의된 관리자 역할만 허용
    private boolean isManagerRole(String role) {
        return "최고관리자".equals(role)
                || "1공정관리자".equals(role)
                || "2공정관리자".equals(role)
                || "3공정관리자".equals(role)
                || "4공정관리자".equals(role)
                || "5공정관리자".equals(role);
    }

    // 역할과 DB의 process_code 연결. 실제 저장값이 다르면 여기만 수정.
    private String processCodeForRole(String role) {
        return switch (role) {
            case "1공정관리자" -> "원료 칭량";
            case "2공정관리자" -> "가열/혼합";
            case "3공정관리자" -> "냉각";
            case "4공정관리자" -> "벌크 검사";
            case "5공정관리자" -> "최종 포장 검사";
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 공정을 확인할 수 없습니다.");
        };
    }

    // 로그인한 사용자가 해당 알림의 공정을 처리할 수 있는지 서버에서 검증
    private void requireProcessPermission(Users_Entity loginUser, Anomaly_event_Entity alarm) {
        if ("최고관리자".equals(loginUser.getRole())) return;
        String allowedProcess = processCodeForRole(loginUser.getRole());
        if (!allowedProcess.equals(alarm.getProcessCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 공정의 알림만 처리할 수 있습니다.");
        }
    }

    // 이상 발생 팝업: 공정관리자는 담당 공정만, 최고관리자는 표시하지 않음
    public List<Anomaly_event_Dto> findActivePopupAnomalies(
            HttpSession session) {

        Users_Entity loginUser = requireLoginManager(session);

        // 최고관리자는 이상 발생 팝업을 받지 않음
        if ("최고관리자".equals(loginUser.getRole())) {
            return List.of();
        }

        // 공정관리자는 담당 공정만 조회
        String processCode = processCodeForRole(loginUser.getRole());

        return anomaly_event_Repository
                .findActivePopupAnomalies(false, processCode)
                .stream()
                .map(Anomaly_event_Dto::from)
                .toList();
    }

    // 최고관리자 조치완료 알림 조회
    @Transactional(readOnly = true)
    public List<Completed_action_notification_Dto> findCompletedActionNotifications(
            HttpSession session) {

        Users_Entity loginUser = requireLoginManager(session);

        if (!"최고관리자".equals(loginUser.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "최고관리자만 조치완료 알림을 조회할 수 있습니다."
            );
        }

        return completed_action_notification_Repository
                .findByRecipientUser_UserIdAndIsReadFalseOrderByCreatedAtAsc(
                        loginUser.getUserId()
                )
                .stream()
                .map(notification -> new Completed_action_notification_Dto(
                        notification.getNotificationId(),
                        Anomaly_event_Dto.from(notification.getAnomalyEvent())
                ))
                .toList();
    }

    // 최고관리자 조치완료 알림 읽음 처리
    @Transactional
    public void markCompletedNotificationAsRead(
            Long notificationId, HttpSession session) {

        Users_Entity loginUser = requireLoginManager(session);

        // 최고관리자만 읽음 처리 가능
        if (!"최고관리자".equals(loginUser.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "최고관리자만 알림을 읽음 처리할 수 있습니다."
            );
        }

        // 알림 조회
        Completed_action_notification_Entity notification =
                completed_action_notification_Repository
                        .findById(notificationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "해당 알림을 찾을 수 없습니다."
                        ));

        // 본인에게 온 알림인지 확인
        if (!loginUser.getUserId().equals(
                notification.getRecipientUser().getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인에게 온 알림만 읽음 처리할 수 있습니다."
            );
        }

        // 읽음 처리
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    // 조치완료 시 최고관리자 알림 생성
    private void createCompletedActionNotifications(
            Anomaly_event_Entity anomaly) {

        // 활성화된 최고관리자 목록 조회
        List<Users_Entity> superAdmins = users_Repository.findAll()
                .stream()
                .filter(user ->
                        "최고관리자".equals(user.getRole())
                        && Boolean.TRUE.equals(user.getIsActive()))
                .toList();

        for (Users_Entity admin : superAdmins) {

            // 동일 알림 중복 생성 방지
            boolean exists = completed_action_notification_Repository
                    .existsByAnomalyEvent_AnomalyIdAndRecipientUser_UserId(
                            anomaly.getAnomalyId(),
                            admin.getUserId()
                    );

            if (exists) {
                continue;
            }

            Completed_action_notification_Entity notification =
                    Completed_action_notification_Entity.builder()
                            .anomalyEvent(anomaly)
                            .recipientUser(admin)
                            .isRead(false)
                            .build();

            completed_action_notification_Repository.save(notification);
        }
    }

    // 1) 개별조회
    public Anomaly_event_Dto findOne(
            Long anomaly_id,
            HttpSession session) {

        // 1. 로그인한 관리자 확인
        Users_Entity loginUser = requireLoginManager(session);

        // 2. 알람번호로 데이터 조회
        Anomaly_event_Entity entity = anomaly_event_Repository
                .findById(anomaly_id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 이상 알람을 찾을 수 없습니다."
                ));

        // 3. 최고관리자는 모든 공정 조회 가능
        boolean isSuperAdmin =
                "최고관리자".equals(loginUser.getRole());

        // 4. 공정관리자는 담당 공정만 조회 가능
        if (!isSuperAdmin) {

            String managerProcessCode =
                    processCodeForRole(loginUser.getRole());

            if (!managerProcessCode.equals(entity.getProcessCode())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "담당 공정의 이상 알람만 조회할 수 있습니다."
                );
            }
        }
        // 5. Entity → DTO 변환
        return Anomaly_event_Dto.from(entity);
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

    public Anomaly_event_Dto confirm(Long anomalyId, Anomaly_event_Confirm_Dto request, HttpSession session) {
        Users_Entity loginUser = requireLoginManager(session);

        // 최고관리자는 이상 알림 확인 처리 불가
        if ("최고관리자".equals(loginUser.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이상 알림 확인은 담당 공정관리자만 할 수 있습니다."
            );
        }

        // 1. 알람번호 확인
        if (anomalyId == null || anomalyId <= 0) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 알람번호를 입력해 주세요.");}

        // 2. 사용자번호 확인
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"확인한 사용자 번호를 입력해 주세요.");}

        // 3. 사용자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);

        if (user == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 사용자가 없습니다.");}
        requireActiveActingManager(user);

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
        requireProcessPermission(loginUser, alarm);

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

            Long anomalyId, Anomaly_event_Action_Dto request, HttpSession session) {
        Users_Entity loginUser = requireLoginManager(session);

        // 최고관리자는 조치하지 않고 처리 내역만 조회
        if ("최고관리자".equals(loginUser.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "조치 완료는 담당 공정관리자만 할 수 있습니다."
            );
        }

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
        requireActiveActingManager(user);

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
        requireProcessPermission(loginUser, alarm);

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

        // 18. 최고관리자에게 조치완료 알림 생성
        createCompletedActionNotifications(saved);

        // 19. 저장된 결과를 DTO로 변환하여 반환
        return Anomaly_event_Dto.from(saved);
    }

    // 수동 입력된 조치 담당자는 활성 관리자여야 한다.
    // 대리 조치를 허용하므로 입력한 관리자의 담당 공정까지 제한하지 않는다.
    private void requireActiveActingManager(Users_Entity user) {
        if (!Boolean.TRUE.equals(user.getIsActive()) || !isManagerRole(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 관리자 사원번호를 입력해 주세요.");
        }
    }

}
