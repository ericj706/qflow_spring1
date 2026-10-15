package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.dto.chart.Chart_Dto;
import springproject.model.dto.chart.Dashboard_Stats_Dto;
import springproject.model.dto.request.Filling_packaging_Result_Dto;
import springproject.model.dto.request.Filling_packaging_Save_Dto;
import springproject.model.dto.search.Filling_packaging_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Filling_packaging_Repository;
import springproject.model.repository.Process_execution_Repository;
import springproject.model.repository.Users_Repository;

@Service
@RequiredArgsConstructor
public class Filling_packaging_Service {

    private final Filling_packaging_Repository filling_packaging_Repository;
    private final Batches_Repository batches_Repository;
    private final Process_execution_Repository process_execution_Repository;
    private final Users_Repository users_Repository;

    // 1. 전체조회 + 조건검색 + 페이징 (초기 진입 시 최근 6개월 범위 기본값 처리)
    public Page<Filling_packaging_Dto> search(
            Filling_packaging_SearchDto searchDto,
            int page) {

        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String batchId = searchDto.getBatchId();
        String packagingLine = searchDto.getPackagingLine();
        String finalDisposition = searchDto.getFinalDisposition();
        String checkweigherStatus = searchDto.getCheckweigherStatus();
        String metalDetectorStatus = searchDto.getMetalDetectorStatus();
        String visionInspectionStatus = searchDto.getVisionInspectionStatus();
        Integer userId = searchDto.getUserId();

        // 💡 페이지 처음 진입 시(날짜 선택 안 함): endDate = 오늘, startDate = 6개월 전 자동 설정
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusMonths(6);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
        }

        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일의 범위를 확인해 주세요.");
        }

        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자 번호는 1 이상이어야 합니다.");
        }

        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) { batchId = null; }
        }
        if (packagingLine != null) {
            packagingLine = packagingLine.trim();
            if (packagingLine.isEmpty()) { packagingLine = null; }
        }
        if (finalDisposition != null) {
            finalDisposition = finalDisposition.trim();
            if (finalDisposition.isEmpty()) { finalDisposition = null; }
        }
        if (checkweigherStatus != null) {
            checkweigherStatus = checkweigherStatus.trim();
            if (checkweigherStatus.isEmpty()) { checkweigherStatus = null; }
        }
        if (metalDetectorStatus != null) {
            metalDetectorStatus = metalDetectorStatus.trim();
            if (metalDetectorStatus.isEmpty()) { metalDetectorStatus = null; }
        }
        if (visionInspectionStatus != null) {
            visionInspectionStatus = visionInspectionStatus.trim();
            if (visionInspectionStatus.isEmpty()) { visionInspectionStatus = null; }
        }

        LocalDateTime startAt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endAtExclusive = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(page, 20);
        Page<Filling_packaging_Entity> result =
                filling_packaging_Repository.search(
                        startAt,
                        endAtExclusive,
                        batchId,
                        packagingLine,
                        finalDisposition,
                        checkweigherStatus,
                        metalDetectorStatus,
                        visionInspectionStatus,
                        userId,
                        pageable
                );

        return result.map(Filling_packaging_Dto::from);
    }

    // 2. 개별조회
    @Transactional(readOnly = true)
    public Filling_packaging_Dto findOne(String pouch_id) {
        return filling_packaging_Repository.findById(pouch_id)
                .map(Filling_packaging_Dto::from)
                .orElseThrow(() -> new java.util.NoSuchElementException("해당 충진/포장 검사 기록을 찾을 수 없습니다. ID: " + pouch_id));
    }

    // 3. 차트조회
    public List<Chart_Dto> getChartSummary(String groupBy, String batchId, String startDate, String endDate) {
        LocalDateTime startAt = null;
        LocalDateTime endAtExclusive = null;

        // 💡 [추가] startDate와 endDate가 모두 비어있거나(null or empty) 전달되지 않은 경우 -> 최근 6개월 범위 자동 지정
        if ((startDate == null || startDate.trim().isEmpty()) && (endDate == null || endDate.trim().isEmpty())) {
            LocalDate today = LocalDate.now();
            LocalDate sixMonthsAgo = today.minusMonths(6);
            startAt = sixMonthsAgo.atStartOfDay();
            endAtExclusive = today.plusDays(1).atStartOfDay();
        } else {
            if (startDate != null && !startDate.trim().isEmpty()) {
                startAt = LocalDate.parse(startDate.trim()).atStartOfDay();
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endAtExclusive = LocalDate.parse(endDate.trim()).plusDays(1).atStartOfDay();
            }
        }

        if ("15min".equalsIgnoreCase(groupBy) || "15분별".equalsIgnoreCase(groupBy)) {
            if (batchId == null || batchId.isEmpty()) {
                return Collections.emptyList();
            }
            return filling_packaging_Repository.find15minSummary(batchId, startAt, endAtExclusive);
        } else if ("hourly".equalsIgnoreCase(groupBy)) {
            return filling_packaging_Repository.findHourlySummary(startAt, endAtExclusive);
        } else if ("lot".equalsIgnoreCase(groupBy)) {
            return filling_packaging_Repository.findLotSummary(startAt, endAtExclusive);
        } else if ("recent5".equalsIgnoreCase(groupBy) || "recentLot".equalsIgnoreCase(groupBy)) {
            return filling_packaging_Repository.findRecent5LotSummary();
        } else {
            return filling_packaging_Repository.findDailySummary(startAt, endAtExclusive);
        }
    }
    
    // 4. 대시보드 카운트 통합 조회
    public Dashboard_Stats_Dto getDashboardStats(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId는 필수입니다.");
        }
        return filling_packaging_Repository.findDashboardStats(batchId.trim());
    }

    // 5. 합/불 개수 요약 조회 (목록 조회 조건과 동일한 6개월 필터 적용)
    public Map<String, Long> getSummary(Filling_packaging_SearchDto searchDto) {
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();

        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusMonths(6);
        }

        LocalDateTime startAt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endAtExclusive = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        List<Object[]> counts = filling_packaging_Repository.countByFinalDispositionGroup(startAt, endAtExclusive);

        long passCount = 0L;
        long failCount = 0L;

        for (Object[] row : counts) {
            if (row[0] == null) continue;

            String resStr = ((String) row[0]).trim();
            Long count = (Long) row[1];

            if ("합격".equals(resStr)) {
                passCount = count;
            } else if ("불합격".equals(resStr)) {
                failCount = count;
            }
        }
        Map<String, Long> map = new HashMap<>();
        map.put("passCount", passCount);
        map.put("failCount", failCount);
        return map;
    }

    // 6. 포장·검사 제품 신규 등록
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Filling_packaging_Dto save(Filling_packaging_Save_Dto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "저장할 데이터를 입력해 주세요.");
        }
        if (request.getPouch_id() == null || request.getPouch_id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제품번호를 입력해 주세요.");
        }

        String pouchId = request.getPouch_id().trim();
        if (pouchId.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제품번호는 100자 이내로 입력해 주세요.");
        }

        Long executionId = request.getExecutionId();
        if (executionId == null || executionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 공정 실행번호를 입력해 주세요.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자 번호를 입력해 주세요.");
        }

        String finalDisposition = request.getFinal_disposition();
        if (finalDisposition != null) {
            finalDisposition = finalDisposition.trim();
            if (finalDisposition.isEmpty()) { finalDisposition = null; }
        }

        if (finalDisposition != null
                && !"합격".equals(finalDisposition)
                && !"불합격".equals(finalDisposition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 판정은 합격 또는 불합격으로 입력해 주세요.");
        }

        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }

        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 LOT가 없습니다.");
        }

        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 공정 실행이 없습니다.");
        }

        if (execution.getBatchesEntity() == null || !batchId.equals(execution.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공정 실행의 LOT 정보가 변경되었습니다. 다시 조회해 주세요.");
        }

        if (!"최종 포장 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "최종 포장 검사 공정에만 제품을 등록할 수 있습니다.");
        }

        if (!"진행중".equals(batch.getStatus()) || !"진행중".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 LOT와 공정에만 제품을 등록할 수 있습니다.");
        }

        if (filling_packaging_Repository.existsById(pouchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 제품번호입니다.");
        }

        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 담당자가 없습니다.");
        }

        LocalDateTime timestamp = request.getTimestamp();
        if (timestamp == null) { timestamp = LocalDateTime.now(); }

        Filling_packaging_Entity entity =
                Filling_packaging_Entity.builder()
                        .pouch_id(pouchId)
                        .batchesEntity(batch)
                        .usersEntity(user)
                        .process_execution_Entity(execution)
                        .packaging_line(request.getPackaging_line())
                        .timestamp(timestamp)
                        .sheet_material_code(request.getSheet_material_code())
                        .sheet_lot_no(request.getSheet_lot_no())
                        .sheet_dry_weight_g(request.getSheet_dry_weight_g())
                        .fill_weight_1st_g(request.getFill_weight_1st_g())
                        .fill_weight_2nd_g(request.getFill_weight_2nd_g())
                        .essence_net_weight_g(request.getEssence_net_weight_g())
                        .pouch_tare_weight_g(request.getPouch_tare_weight_g())
                        .gross_total_weight_g(request.getGross_total_weight_g())
                        .upper_seal_temp_c(request.getUpper_seal_temp_c())
                        .lower_seal_temp_c(request.getLower_seal_temp_c())
                        .seal_pressure_bar(request.getSeal_pressure_bar())
                        .n2_residual_o2_pct(request.getN2_residual_o2_pct())
                        .checkweigher_status(request.getCheckweigher_status())
                        .metal_detector_status(request.getMetal_detector_status())
                        .vision_inspection_status(request.getVision_inspection_status())
                        .final_disposition(finalDisposition)
                        .record_source("수동입력")
                        .note(request.getNote())
                        .build();

        Filling_packaging_Entity saved = filling_packaging_Repository.save(entity);
        return Filling_packaging_Dto.from(saved);
    }

    // 7. 제품의 검사 결과, 판정 및 비고 수정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Filling_packaging_Dto updateResult(
            String pouchId,
            Filling_packaging_Result_Dto request) {

        if (pouchId == null || pouchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제품번호를 입력해 주세요.");
        }
        pouchId = pouchId.trim();

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 검사 결과를 입력해 주세요.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자 번호를 입력해 주세요.");
        }

        String finalDisposition = request.getFinal_disposition();
        if (finalDisposition == null || finalDisposition.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 판정을 입력해 주세요.");
        }
        finalDisposition = finalDisposition.trim();
        if (!"합격".equals(finalDisposition) && !"불합격".equals(finalDisposition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 판정은 합격 또는 불합격으로 입력해 주세요.");
        }

        Long executionId = filling_packaging_Repository.findExecutionIdByPouchId(pouchId).orElse(null);
        if (executionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제품이 없거나 공정 실행이 연결되지 않았습니다.");
        }

        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }

        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 LOT가 없습니다.");
        }

        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 공정 실행이 없습니다.");
        }

        Filling_packaging_Entity entity = filling_packaging_Repository.findByIdForUpdate(pouchId).orElse(null);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 제품이 없습니다.");
        }

        if (execution.getBatchesEntity() == null || !batchId.equals(execution.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "공정 실행의 LOT 정보가 변경되었습니다. 다시 조회해 주세요.");
        }
        if (entity.getProcess_execution_Entity() == null || !executionId.equals(entity.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제품의 공정 실행 정보가 변경되었습니다. 다시 조회해 주세요.");
        }
        if (entity.getBatchesEntity() == null || !batchId.equals(entity.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제품과 공정 실행의 LOT가 다릅니다.");
        }

        if (!"최종 포장 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "최종 포장 검사 공정에서만 검사 결과를 수정할 수 있습니다.");
        }
        if (!"진행중".equals(batch.getStatus()) || !"진행중".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LOT 또는 공정이 진행 중이 아니므로 수정할 수 없습니다.");
        }

        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 담당자가 없습니다.");
        }

        String checkweigherStatus = request.getCheckweigher_status();
        String metalDetectorStatus = request.getMetal_detector_status();
        String visionInspectionStatus = request.getVision_inspection_status();

        if (checkweigherStatus != null) {
            checkweigherStatus = checkweigherStatus.trim();
            if (checkweigherStatus.isEmpty() || checkweigherStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중량검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setCheckweigher_status(checkweigherStatus);
        }

        if (metalDetectorStatus != null) {
            metalDetectorStatus = metalDetectorStatus.trim();
            if (metalDetectorStatus.isEmpty() || metalDetectorStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "금속검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setMetal_detector_status(metalDetectorStatus);
        }

        if (visionInspectionStatus != null) {
            visionInspectionStatus = visionInspectionStatus.trim();
            if (visionInspectionStatus.isEmpty() || visionInspectionStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비전검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setVision_inspection_status(visionInspectionStatus);
        }

        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }

        entity.setFinal_disposition(finalDisposition);
        entity.setUsersEntity(user);

        Filling_packaging_Entity saved = filling_packaging_Repository.save(entity);
        return Filling_packaging_Dto.from(saved);
    }

    // 불합격 제품 한 건의 폐기 확정
    // loginUserId는 Controller에서 로그인 세션으로 확인하여 전달
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Filling_packaging_Dto discardProduct(String pouchId,Integer loginUserId) {
        // 1. 로그인 확인
        if (loginUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인 후 폐기를 진행해 주세요.");
        }
        // 2. 활성 사용자 및 관리자 권한 확인
        Users_Entity user = users_Repository.findById(loginUserId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"활성 사용자만 폐기를 확정할 수 있습니다.");
        }

        String role = user.getRole();
        if (!"4공정관리자".equals(role) && !"5공정관리자".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"4공정관리자 또는 5공정관리자만 폐기를 확정할 수 있습니다.");
        }
        // 3. 제품번호 확인
        if (pouchId == null || pouchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"제품번호를 입력해 주세요.");
        }
        pouchId = pouchId.trim();

        // 4. 제품에 연결된 실행번호와 LOT 번호 조회
        Long executionId = filling_packaging_Repository.findExecutionIdByPouchId(pouchId).orElse(null);
        if (executionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"제품이 없거나 공정 실행이 연결되지 않았습니다.");
        }
        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }

        // 5. 기존 처리와 같은 순서로 잠금: LOT → 공정 실행 → 제품
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND,"해당 공정 실행이 없습니다.");
        }
        Filling_packaging_Entity product = filling_packaging_Repository.findByIdForUpdate(pouchId).orElse(null);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 제품이 없습니다.");
        }

        // 6. 잠금 이후 LOT·공정·제품 연결 재확인
        if (execution.getBatchesEntity() == null || !batchId.equals(execution.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 실행의 LOT 연결을 확인해 주세요.");
        }
        if (product.getBatchesEntity() == null || !batchId.equals(product.getBatchesEntity().getBatchId())
                || product.getProcess_execution_Entity() == null
                || !executionId.equals(product.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"제품의 LOT 또는 공정 실행 연결이 변경되었습니다.");
        }
        // 7. 최종 포장 검사에서 불합격한 제품만 대상
        if (!"최종 포장 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 포장 검사 제품만 폐기할 수 있습니다.");
        }
        if (!"불합격".equals(product.getFinal_disposition())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"종합결과가 불합격인 제품만 폐기할 수 있습니다.");
        }

        // 8. 이미 폐기 확정된 제품은 최초 처리자·시간을 유지
        // LOT 완료 후 동일 요청이 다시 와도 기존 결과 반환
        if ("폐기완료".equals(product.getDisposalStatus())) {return Filling_packaging_Dto.from(product);}

        // 9. 공정 판정이 끝나고 LOT 완료를 기다리는 상태에서만 확정
        if (!"진행중".equals(batch.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"진행중 상태의 LOT에서만 제품 폐기를 확정할 수 있습니다.");
        }
        if (!"완료".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 포장 공정을 종료하고 판정을 확인한 후 폐기해 주세요."
            );
        }

        // 기존 미설정 기록도 불합격 판정과 공정 상태를 확인하여 처리
        String disposalStatus = product.getDisposalStatus();
        if (disposalStatus != null && !disposalStatus.isBlank() && !"폐기대기".equals(disposalStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"확인할 수 없는 제품 폐기 상태입니다.");
        }

        // 10. 제품별 폐기 상태·처리자·시간 저장
        product.setDisposalStatus("폐기완료");
        product.setDisposedByUser(user);
        product.setDisposedAt(LocalDateTime.now());

        // final_disposition은 불합격 유지
        // 기존 제품 담당자, 측정값, 검사 결과, 알람 이력도 유지
        // 트랜잭션 종료 시 변경 내용 저장
        return Filling_packaging_Dto.from(product);
    }

    // 현재 공정 실행의 제품 목록 조회
    @Transactional(readOnly = true)
    public Page<Filling_packaging_Dto> findByExecutionId(
            Long executionId,
            int page) {
        if (executionId == null || executionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");
        }
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(Sort.Order.desc("timestamp"),Sort.Order.asc("pouch_id"))
        );

        return filling_packaging_Repository
                .findPageByExecutionId(executionId, pageable)
                .map(Filling_packaging_Dto::from);
}

}