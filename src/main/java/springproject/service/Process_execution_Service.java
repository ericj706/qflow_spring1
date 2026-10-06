package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.repository.Process_execution_Repository;

@Service
@RequiredArgsConstructor

public class Process_execution_Service {
    private final Process_execution_Repository pr;

    // 전체조회 + 조건검색 + 페이징
    public Page<Process_execution_Dto> search(
            Process_execution_SearchDto searchDto,
            int page
    ) {
        // 페이지 번호 검증
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"페이지 번호는 0 이상이어야 합니다.");
        }

        LocalDate startDate = searchDto.getStartDate();
        LocalDate endDate = searchDto.getEndDate();
        // 시작일과 종료일 검증
        if (startDate != null
                && endDate != null
                && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작일은 종료일보다 늦을 수 없습니다.");
        }
        // 종료일에 하루를 더할 수 있는지 검증
        if (LocalDate.MAX.equals(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,"종료일 범위를 확인해 주세요.");
        }

        // 생산 LOT 번호
        String batchId = searchDto.getBatchId();
        if (batchId != null) {
            batchId = batchId.trim();
            if (batchId.isEmpty()) {batchId = null;}
        }

        // 공정코드
        String processCode = searchDto.getProcessCode();
        if (processCode != null) {
            processCode = processCode.trim();
            if (processCode.isEmpty()) {processCode = null;}
        }

        // 공정 진행상태
        String status = searchDto.getStatus();
        if (status != null) {
            status = status.trim();
            if (status.isEmpty()) {status = null;}
        }

        // 시작일 00:00 이상
        LocalDateTime startAt = null;
        if (startDate != null) {startAt = startDate.atStartOfDay();}
        // 종료일 다음 날 00:00 미만
        // 종료일 당일의 모든 시간을 포함
        LocalDateTime endAtExclusive = null;
        if (endDate != null) {endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 페이지당 20개 고정
        // 정렬은 Repository의 ORDER BY 사용
        Pageable pageable = PageRequest.of(page, 20);

        // 현재 페이지에 해당하는 공정 실행이력 조회
        Page<Process_execution_Entity> entities =
                pr.search(
                        startAt,
                        endAtExclusive,
                        batchId,
                        processCode,
                        status,
                        pageable
                );

        // Entity → DTO 변환
        // 전체 건수와 페이지 정보 유지
        return entities.map(Process_execution_Dto::from);
    }
    
    // 개별조회 : PK(execution_id)로 조회한 후 DTO로 변환
    public Process_execution_Dto findOne(Long execution_id){

        Process_execution_Entity entity
            = pr.findById(execution_id).orElse(null);
            
        // 해당 PK의 데이터가 없으면 null 반환
        if(entity == null){
            return null;
        }

        return Process_execution_Dto.from(entity);
    }
}