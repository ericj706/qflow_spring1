package springproject.service;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Bulk_qc_SearchDto;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.repository.Bulk_qc_Repository;

@Service 
@RequiredArgsConstructor 
public class Bulk_qc_Service {
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
    
    // 최종 검사결과 수정
    @Transactional 
    public  Bulk_qc_Dto updateResult(String qc_id, Bulk_qc_Dto dto){
        // qc_id로 기존 엔티티 조회
        Bulk_qc_Entity entity = bulk_qc_Repository.findById(qc_id)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 검사기록: "+qc_id));
        // 종합검사결과 수정
        if(dto.getOverall_qc_result() != null && !dto.getOverall_qc_result().trim().isEmpty()){
            entity.setOverall_qc_result(dto.getOverall_qc_result());
        }
        // 비고/사유 수정 및 저장
        if (dto.getQc_notes_code() != null) {
            entity.setQc_notes_code(dto.getQc_notes_code());
        }
        return Bulk_qc_Dto.from(entity);
    }

    @Transactional 
    public Bulk_qc_Dto deleteResult(String qc_id){
        Bulk_qc_Entity entity = bulk_qc_Repository.findById(qc_id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 검사기록: "+qc_id));
        entity.setOverall_qc_result(null);
        entity.setQc_notes_code(null);
        return Bulk_qc_Dto.from(entity);    
    }

}
