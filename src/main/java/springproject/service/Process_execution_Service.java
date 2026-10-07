package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.request.Process_execution_Start_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.model.entity.Batches_Entity;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.repository.Batches_Repository;
import springproject.model.repository.Process_execution_Repository;


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
 
    // 공정 등록 ( 공정 시작 )
    @Transactional
    public Process_execution_Dto save(Process_execution_Start_Dto request){
        // 1. LOT 번호 확인
        if(request == null || request.getBatchId() == null || request.getBatchId().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LOT 번호를 입력해 주세요.");
        }
        // 2. 공정명 확인
        if(request.getProcessCode() == null || request.getProcessCode().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"시작할 공정을 선택");
        }

        String batchId = request.getBatchId().trim();
        String processCode = request.getProcessCode().trim();
        // 3. 사용할 수 있는 공정인지 확인
        Set<String> allowedProcesses = Set.of(
            "원료 칭량", "가열/혼합", "냉각", "벌크 검사", "최종 포장 검사"
        );
        if( !allowedProcesses.contains(processCode)){ // 정의된 공정의 포함되지 않다면
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용할 수 없는 공정");
        }

        // 4. LOT 조회 및 잠금
        Batches_Entity batch = batches_Repository.findByIdForUpdate(batchId).orElse(null);
        if( batch == null){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 LOT 없음"); }
        
        // 5. 같은 LOT에서 같은 공정이 진행 중인지 확인 --> 존재하면 숫자로 나옴
        long count = pr.countByBatchAndProcessAndStatus(batchId, processCode, "진행중");
        if( count > 0){throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 LOT는 이미 진행 중인 공정임");}

        // 6. 새로운 공정 실행 기록 생성
        Process_execution_Entity entity = Process_execution_Entity.builder()
                                            .batchesEntity(batch).process_code(processCode)
                                            .start_time(LocalDateTime.now()).status("진행중")
                                            .record_source("수동입력").build();
        
        // 7. DB 저장
        Process_execution_Entity saved = pr.save(entity);
        return Process_execution_Dto.from(saved); 

    } 

}