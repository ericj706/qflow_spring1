package springproject.service;

import java.math.BigDecimal;
import java.util.Set;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.entity.Users_Entity;
import springproject.model.repository.Users_Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Bulk_qc_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Bulk_qc_Repository;
import springproject.model.repository.Process_execution_Repository;

@Service 
@RequiredArgsConstructor 
public class Bulk_qc_Service {
    private final Users_Repository users;

    private final Bulk_qc_Repository bulk_qc_Repository;

    // 전체조회 + 조건검색 + 페이징 + (새로 작성하신 그룹쿼리로 합격/불합격 집계)
    public Page<Bulk_qc_Dto> search(Bulk_qc_SearchDto searchDto, int page) {
        // 페이지 번호 확인
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }

        // 검색조건 꺼내기
        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        String batchId = searchDto.getBatchId();
        String overallQcResult = searchDto.getOverallQcResult();
        Integer userId = searchDto.getUserId();

        // 검색기간 확인
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
        }

        // 종료일에 하루를 더할 수 있는지 확인
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일의 범위를 확인해 주세요.");
        }

        // 담당자 번호 확인
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자 번호는 1 이상이어야 합니다.");
        }

        // LOT 번호: 앞뒤 공백 제거, 빈 문자열은 null 처리
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) { batchId = null; }
        }

        // 검사결과: 앞뒤 공백 제거, 빈 문자열은 null 처리
        if (overallQcResult != null) {
            overallQcResult = overallQcResult.trim();
            if (overallQcResult.isEmpty()) { overallQcResult = null; }
        }

        // 시작일 당일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) { startAt = startDate.atStartOfDay(); }

        // 종료일 다음 날 00:00 미만 (종료일 당일의 모든 시간을 포함)
        LocalDateTime endAtExclusive = null;
        if (endDate != null) { endAtExclusive = endDate.plusDays(1).atStartOfDay(); }

        // 한 페이지당 20개 고정
        Pageable pageable = PageRequest.of(page, 20);

        // 기존 조건검색 + 페이징 조회
        Page<Bulk_qc_Entity> result = bulk_qc_Repository.search(
                startAt,
                endAtExclusive,
                batchId,
                overallQcResult,
                userId,
                pageable
        );
        return result.map(Bulk_qc_Dto::from);
    }
    public Map<String, Long> getSummary() {
        List<Object[]> counts = bulk_qc_Repository.countByOverallQcResultGroup();

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

        // Map<String, Long>으로 명확히 지정
        Map<String, Long> summary = new HashMap<>();
        summary.put("passCount", passCount);
        summary.put("failCount", failCount);
        return summary;
    }

    // 개별 조회 PK
    public Bulk_qc_Dto findOne(String qc_id){
        Optional<Bulk_qc_Entity> optional = bulk_qc_Repository.findById(qc_id);
        if(optional.isPresent()){
            Bulk_qc_Entity bulk_qc_Entity = optional.get();
            return Bulk_qc_Dto.from(bulk_qc_Entity);
        }
        return null;
    }   
    
    // 최종 검사결과 및 비고 수정
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Bulk_qc_Dto updateQcResult(String qc_id, Bulk_qc_Dto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정할 내용을 입력해 주세요.");
        }
        // LOT 잠금 및 승인 여부 확인 후 변경 가능한 검사 반환
        Bulk_qc_Entity entity = findEditableQc(qc_id);
        String result = dto.getOverall_qc_result();
        if (result != null && !result.isBlank()) {
            result = result.trim();
            if (!"합격".equals(result) && !"불합격".equals(result)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"검사 결과는 합격 또는 불합격으로 입력해 주세요.");
            }
            entity.setOverall_qc_result(result);
        }
        if (dto.getQc_notes_code() != null) {entity.setQc_notes_code(dto.getQc_notes_code());}
        // 트랜잭션 종료 시 변경 내용 저장
        return Bulk_qc_Dto.from(entity);
    }

    // 검사결과와 비고 초기화 — 검사 행 자체는 삭제하지 않음
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Bulk_qc_Dto deleteResult(String qc_id) {
        Bulk_qc_Entity entity = findEditableQc(qc_id);
        entity.setOverall_qc_result(null);
        entity.setQc_notes_code(null);
        return Bulk_qc_Dto.from(entity);
    }

    // 해당 공정 실행에 연결된 검사 결과 목록
    @Transactional (readOnly = true)
    public List<Bulk_qc_Dto> findByExecutionId(Long executionId){
        if(executionId == null ||  executionId <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 공정 실행번호를 입력해");
        }
        return bulk_qc_Repository.findByExecutionId(executionId).stream().map(Bulk_qc_Dto::from).toList();
    }

    private final Batches_Repository batches_Repository;
    private final Process_execution_Repository process_execution_Repository;

    // 변경할 검사 조회 및 최종 승인 여부 확인
    private Bulk_qc_Entity findEditableQc(String qcId) {
        if (qcId == null || qcId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"검사번호를 입력해 주세요.");
        }

        // 엔티티를 읽기 전에 LOT 번호부터 조회
        String batchId = bulk_qc_Repository.findBatchIdByQcId(qcId).orElse(null);
        if (batchId == null) {
            if (!bulk_qc_Repository.existsById(qcId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 검사 기록이 없습니다.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,"검사 기록에 연결된 LOT를 확인해 주세요.");
        }

        // 공정 종료·재개·승인 처리와 동일한 LOT 잠금 사용
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 LOT가 없습니다.");
        }
        // 잠금을 얻은 뒤 검사 기록 조회
        Bulk_qc_Entity entity = bulk_qc_Repository.findById(qcId).orElse(null);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"해당 검사 기록이 없습니다.");
        }
        if (entity.getBatchesEntity() == null || !batchId.equals(entity.getBatchesEntity().getBatchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"검사의 LOT 연결이 변경되었습니다. 다시 조회해 주세요.");
        }
        Process_execution_Entity execution = entity.getProcess_execution_Entity();
        if (execution != null) {
            // 추가: 다음 공정이 이미 생성됐다면 수정·초기화 금지
            Process_execution_Entity nextExecution = process_execution_Repository.findNextByExecutionId(execution.getExecution_id()).orElse(null);
            if (nextExecution != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"다음 공정이 이미 시작되어 벌크 검사 결과를 "+ "수정하거나 초기화할 수 없습니다.");
            }

            // 기존: 자동 합격에 사용된 기록은 수정·초기화 금지
            if (Boolean.TRUE.equals(execution.getBulkQcAutoPassed())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"자동 합격에 사용된 검사 결과는 "+ "수정하거나 초기화할 수 없습니다.");
            }
        }

        // 기존: 수동 최종 승인에 사용된 기록은 수정·초기화 금지
        long approvedCount = process_execution_Repository.countByFinalQcId(qcId);
        if (approvedCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"최종 승인된 검사 결과는 수정하거나 초기화할 수 없습니다.");
        }
        return entity;
    }


    // 벌크 검사 원본을 실제 LOT·공정·작업자에 연결한다. 종합 판정은 별도 결과 API에서 처리한다.
    @Transactional
    public Bulk_qc_Dto save(Bulk_qc_Dto dto) {
        if (dto == null || dto.getQc_id() == null || dto.getQc_id().isBlank() || dto.getQc_id().length() > 50
            || dto.getSample_time() == null || dto.getPh_measured() == null || dto.getViscosity_measured() == null) {
            throw error(HttpStatus.BAD_REQUEST, "검사번호, 검사시간, pH와 점도가 필요합니다.");
        }
        Process_execution_Entity execution = running(dto.getExecutionId());
        if (!"벌크 검사".equals(execution.getProcess_code())
            || !execution.getBatchesEntity().getBatchId().equals(dto.getBatchId())) {
            throw error(HttpStatus.CONFLICT, "벌크 검사 공정과 LOT를 확인해 주세요.");
        }
        if (bulk_qc_Repository.existsById(dto.getQc_id())) throw error(HttpStatus.CONFLICT, "이미 저장된 검사번호입니다.");
        if (dto.getOverall_qc_result() != null && !Set.of("합격", "불합격").contains(dto.getOverall_qc_result())) {
            throw error(HttpStatus.BAD_REQUEST, "종합 판정은 합격, 불합격 또는 null이어야 합니다.");
        }
        decimal(dto.getPh_measured(), 6, 3);
        decimal(dto.getViscosity_measured(), 12, 3);
        decimal(dto.getSpecific_gravity(), 8, 4);
        text(dto.getAppearance_code(), 50);
        text(dto.getMicrobubble_code(), 50);
        text(dto.getQc_notes_code(), 100);
        text(dto.getRecord_source(), 20);
        return Bulk_qc_Dto.from(bulk_qc_Repository.save(dto.toEntity(execution.getBatchesEntity(), worker(dto.getUserId()), execution)));
    }

    // 기존 공정 서비스와 같은 순서로 LOT·공정을 잠그고 진행 상태를 검사한다.
    private Process_execution_Entity running(Long id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "실제 공정 실행번호가 필요합니다.");
        String batchId = process_execution_Repository.findBatchIdByExecutionId(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "공정 실행 기록이 없습니다."));
        var batch = batches_Repository.findByIdForUpdate(batchId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "LOT가 없습니다."));
        var execution = process_execution_Repository.findByIdForUpdate(id)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "공정 실행 기록이 없습니다."));
        if (!"진행중".equals(batch.getStatus()) || !"진행중".equals(execution.getStatus())) {
            throw error(HttpStatus.CONFLICT, "진행 중인 LOT와 공정에만 데이터를 저장할 수 있습니다.");
        }
        return execution;
    }

    // 저장 요청의 담당자가 실제 존재하는지 확인한다.
    private Users_Entity worker(Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "작업자 번호가 필요합니다.");
        return users.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "작업자가 없습니다."));
    }

    // DB DECIMAL 컬럼의 자릿수를 초과하는 입력을 저장 전에 거절한다.
    private void decimal(BigDecimal value, int precision, int scale) {
        if (value != null && (value.stripTrailingZeros().scale() > scale
            || value.abs().compareTo(BigDecimal.TEN.pow(precision - scale)) >= 0)) {
            throw error(HttpStatus.BAD_REQUEST, "측정값의 소수점 또는 정수 자릿수를 확인해 주세요.");
        }
    }

    // 문자열이 저장 컬럼 길이를 넘으면 입력 오류로 반환한다.
    private void text(String value, int limit) {
        if (value != null && value.length() > limit) throw error(HttpStatus.BAD_REQUEST, "검사 항목 문자열이 너무 깁니다.");
    }

    // 입력·상태 오류에 알맞은 HTTP 응답을 만든다.
    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
