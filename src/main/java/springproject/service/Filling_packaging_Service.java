package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // 전체조회 + 조건검색 + 페이징
    public Page<Filling_packaging_Dto> search(
            Filling_packaging_SearchDto searchDto,
            int page) {
        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String batchId = searchDto.getBatchId();
        String packagingLine = searchDto.getPackagingLine();
        String finalDisposition = searchDto.getFinalDisposition();
        String checkweigherStatus = searchDto.getCheckweigherStatus();
        String metalDetectorStatus = searchDto.getMetalDetectorStatus();
        String visionInspectionStatus = searchDto.getVisionInspectionStatus();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null&& endDate != null&& startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"종료일의 범위를 확인해 주세요.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호는 1 이상이어야 합니다.");
        }

        // 문자열 앞뒤 공백 제거
        // 빈 문자열은 검색조건에서 제외하도록 null 처리
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }
        if (packagingLine != null) {
            packagingLine = packagingLine.trim();
            if (packagingLine.isEmpty()) {packagingLine = null;}
        }
        if (finalDisposition != null) {
            finalDisposition = finalDisposition.trim();
            if (finalDisposition.isEmpty()) {finalDisposition = null;}
        }
        if (checkweigherStatus != null) {
            checkweigherStatus = checkweigherStatus.trim();
            if (checkweigherStatus.isEmpty()) {checkweigherStatus = null;}
        }
        if (metalDetectorStatus != null) {
            metalDetectorStatus = metalDetectorStatus.trim();
            if (metalDetectorStatus.isEmpty()) {metalDetectorStatus = null;}
        }
        if (visionInspectionStatus != null) {
            visionInspectionStatus = visionInspectionStatus.trim();
            if (visionInspectionStatus.isEmpty()) {visionInspectionStatus = null;}
        }

        // 시작일 당일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {startAt = startDate.atStartOfDay();}
        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 한 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY에서 처리
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

        // 페이지 정보를 유지하면서 Entity → DTO 변환
        return result.map(Filling_packaging_Dto::from);
    }

    // 개별조회
    @Transactional(readOnly = true)
    public Filling_packaging_Dto findOne(String pouch_id) {
        return filling_packaging_Repository.findById(pouch_id)
                .map(Filling_packaging_Dto::from)
                .orElseThrow(() -> new java.util.NoSuchElementException("해당 충진/포장 검사 기록을 찾을 수 없습니다. ID: " + pouch_id));
    }
    
    // 차트조회
    public List<Chart_Dto> getChartSummary(String groupBy, String batchId, String startDate, String endDate) {
        // 날짜 문자열 → LocalDateTime 변환 (exclusive upper bound)
        LocalDateTime startAt = null;
        LocalDateTime endAtExclusive = null;

        if (startDate != null && !startDate.trim().isEmpty()) {
            startAt = LocalDate.parse(startDate.trim()).atStartOfDay();
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            endAtExclusive = LocalDate.parse(endDate.trim()).plusDays(1).atStartOfDay();
        }

        if ("15min".equalsIgnoreCase(groupBy) || "15분별".equalsIgnoreCase(groupBy)) {
            if (batchId == null || batchId.isEmpty()) {
                return Collections.emptyList(); // batchId가 없으면 빈 리스트 반환
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

    // 대시보드 카운트 통합 조회
    public Dashboard_Stats_Dto getDashboardStats(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId는 필수입니다.");
        }
        return filling_packaging_Repository.findDashboardStats(batchId.trim());
    }

        // 합/불 개수만 조회
    public Map<String, Long> getSummary() {
        List<Object[]> counts = filling_packaging_Repository.countByFinalDispositionGroup();

        long passCount = 0L;
        long failCount = 0L;

        for (Object[] row : counts) {
            String resStr = (String) row[0];
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
    // 최종합/불결과 및 비고 수정
    @Transactional 
    public Filling_packaging_Dto fixResult(String pouch_id, Filling_packaging_Dto dto) {
        Filling_packaging_Entity entity = filling_packaging_Repository.findById(pouch_id)
                .orElseThrow(() -> new IllegalArgumentException("해당 id를 찾을 수 없습니다: " + pouch_id));
        if  (dto.getFinal_disposition() != null){
            entity.setFinal_disposition(dto.getFinal_disposition());
        }
        //if (dto.getNote() != null){
          //  entity.setNote(dto.getNote());
        //}
        return Filling_packaging_Dto.from(entity);
    }
        
        

    // 최종결과 및 비고 삭제 
    @Transactional 
    public Filling_packaging_Dto deleteResult(String pouch_id){
        Filling_packaging_Entity entity = filling_packaging_Repository.findById(pouch_id)
                .orElseThrow(() -> new IllegalArgumentException("해당 id를 찾을 수 없습니다: " + pouch_id));
        entity.setFinal_disposition(null);
        // entity.setNote(null);
        return Filling_packaging_Dto.from(entity);   
    }

    private final Batches_Repository batches_Repository;
    private final Process_execution_Repository process_execution_Repository;
    private final Users_Repository users_Repository;

    
    // 포장·검사 제품 신규 등록
    @Transactional (isolation = Isolation.READ_COMMITTED)
    public Filling_packaging_Dto save(Filling_packaging_Save_Dto request) {
        // 1. 요청값 확인
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"저장할 데이터를 입력해 주세요.");
        }
        if (request.getPouch_id() == null || request.getPouch_id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"제품번호를 입력해 주세요."
            );
        }

        String pouchId = request.getPouch_id().trim();
        if (pouchId.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"제품번호는 100자 이내로 입력해 주세요.");
        }

        Long executionId = request.getExecutionId();
        if (executionId == null || executionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해 주세요.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호를 입력해 주세요.");
        }

        // 2. 최종 판정 확인
        // null 또는 빈 문자열은 미판정 상태로 저장
        String finalDisposition = request.getFinal_disposition();
        if (finalDisposition != null) {
            finalDisposition = finalDisposition.trim();
            if (finalDisposition.isEmpty()) {finalDisposition = null;}
        }

        if (finalDisposition != null
                && !"합격".equals(finalDisposition)
                && !"불합격".equals(finalDisposition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"최종 판정은 합격 또는 불합격으로 입력해 주세요.");
        }

        // 3. 공정 실행에 연결된 LOT 번호 조회
        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }

        // 4. LOT 조회 및 잠금
        // 공정 종료 처리와 동일하게 LOT → 공정 실행 순서로 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }

        // 5. 공정 실행 조회 및 잠금
        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행이 없습니다.");
        }

        // 공정 실행과 LOT의 연결 확인
        if (execution.getBatchesEntity() == null || !batchId.equals( execution.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 실행의 LOT 정보가 변경되었습니다. 다시 조회해 주세요.");
        }

        // 6. 최종 포장 검사 공정인지 확인
        if (!"최종 포장 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 포장 검사 공정에만 제품을 등록할 수 있습니다.");
        }

        // 7. LOT와 공정 모두 진행 중이어야 등록 가능
        if (!"진행중".equals(batch.getStatus()) || !"진행중".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"진행 중인 LOT와 공정에만 제품을 등록할 수 있습니다.");
        }

        // 8. 제품번호 중복 확인
        if (filling_packaging_Repository.existsById(pouchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 등록된 제품번호입니다.");
        }

        // 9. 담당자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 담당자가 없습니다.");
        }

        // 10. 검사시간 설정
        // 입력한 검사시간이 없으면 현재 시각 사용
        LocalDateTime timestamp = request.getTimestamp();
        if (timestamp == null) { timestamp = LocalDateTime.now();}

        // 11. Entity 생성
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
                        .build();

        // 12. 저장 후 DTO로 반환
        Filling_packaging_Entity saved = filling_packaging_Repository.save(entity);
        return Filling_packaging_Dto.from(saved);
    }

    // 등록된 제품의 검사 결과 수정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Filling_packaging_Dto updateResult(
            String pouchId,
            Filling_packaging_Result_Dto request) {
        // 1. 제품번호 확인
        if (pouchId == null || pouchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"제품번호를 입력해 주세요.");
        }
        pouchId = pouchId.trim();

        // 2. 요청값과 담당자 번호 확인
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정할 검사 결과를 입력해 주세요.");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"담당자 번호를 입력해 주세요.");
        }

        // 3. 최종 판정 확인
        String finalDisposition = request.getFinal_disposition();
        if (finalDisposition == null || finalDisposition.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"최종 판정을 입력해 주세요.");
        }
        finalDisposition = finalDisposition.trim();
        if (!"합격".equals(finalDisposition) && !"불합격".equals(finalDisposition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"최종 판정은 합격 또는 불합격으로 입력해 주세요.");
        }

        // 4. 제품에 연결된 공정 실행번호 조회
        Long executionId = filling_packaging_Repository.findExecutionIdByPouchId(pouchId).orElse(null);
        if (executionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"제품이 없거나 공정 실행이 연결되지 않았습니다.");
        }

        // 5. 공정 실행에 연결된 LOT 번호 조회
        String batchId = process_execution_Repository.findBatchIdByExecutionId(executionId).orElse(null);
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"공정 실행 기록 또는 연결된 LOT가 없습니다.");
        }

        // 6. LOT 조회 및 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }

        // 7. 공정 실행 조회 및 잠금
        Process_execution_Entity execution = process_execution_Repository.findByIdForUpdate(executionId).orElse(null);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 공정 실행이 없습니다.");
        }

        // 8. 제품 조회 및 잠금
        Filling_packaging_Entity entity = filling_packaging_Repository.findByIdForUpdate(pouchId).orElse(null);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 제품이 없습니다.");
        }

        // 9. 잠금 후 공정 실행과 LOT의 연결을 다시 확인
        if (execution.getBatchesEntity() == null || !batchId.equals(execution.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 실행의 LOT 정보가 변경되었습니다. 다시 조회해 주세요.");
        }
        if (entity.getProcess_execution_Entity() == null || !executionId.equals( entity.getProcess_execution_Entity().getExecution_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"제품의 공정 실행 정보가 변경되었습니다. 다시 조회해 주세요.");
        }
        if (entity.getBatchesEntity() == null || !batchId.equals(entity.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"제품과 공정 실행의 LOT가 다릅니다.");
        }

        // 10. 진행 중인 최종 포장 검사에서만 수정 가능
        if (!"최종 포장 검사".equals(execution.getProcess_code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 포장 검사 공정에서만 검사 결과를 수정할 수 있습니다.");
        }
        if (!"진행중".equals(batch.getStatus())
                || !"진행중".equals(execution.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"LOT 또는 공정이 진행 중이 아니므로 수정할 수 없습니다.");
        }

        // 11. 담당자 조회
        Users_Entity user = users_Repository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 담당자가 없습니다.");
        }

        // 12. 세부 검사 결과 확인
        // null이면 기존 값을 유지하고, 값이 있으면 검증 후 변경
        String checkweigherStatus = request.getCheckweigher_status();
        String metalDetectorStatus = request.getMetal_detector_status();
        String visionInspectionStatus = request.getVision_inspection_status();
        if (checkweigherStatus != null) {
            checkweigherStatus = checkweigherStatus.trim();
            if (checkweigherStatus.isEmpty()|| checkweigherStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"중량검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setCheckweigher_status(checkweigherStatus);
        }

        if (metalDetectorStatus != null) {
            metalDetectorStatus = metalDetectorStatus.trim();
            if (metalDetectorStatus.isEmpty() || metalDetectorStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"금속검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setMetal_detector_status(metalDetectorStatus);
        }

        if (visionInspectionStatus != null) {
            visionInspectionStatus = visionInspectionStatus.trim();
            if (visionInspectionStatus.isEmpty() || visionInspectionStatus.length() > 40) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"비전검사 결과는 빈 값이 아닌 40자 이내로 입력해 주세요.");
            }
            entity.setVision_inspection_status(visionInspectionStatus);
        }

        // 13. 최종 판정과 담당자 변경
        entity.setFinal_disposition(finalDisposition);
        entity.setUsersEntity(user);
        // 14. 저장 후 DTO로 반환
        Filling_packaging_Entity saved = filling_packaging_Repository.save(entity);
        return Filling_packaging_Dto.from(saved);
    }



}
