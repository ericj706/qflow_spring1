package springproject.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
import springproject.model.dto.Process_execution_Finish_Dto;
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

    // 공정 종료  )) 조회 -> 상태를 변경 후 소요시간과 함께 저장
    @Transactional 
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

        // 6. 시작시간 확인 
        LocalDateTime startTime = entity.getStart_time();
        LocalDateTime endTime = LocalDateTime.now();
        if(startTime == null || startTime.isAfter(endTime)){throw new ResponseStatusException(HttpStatus.CONFLICT,"공정 시작시간을 확인해 주세요.");}
        // 7. 소요시간 분 단위 계산
        Long elapsedMillis = Duration.between(startTime, endTime).toMillis();
        BigDecimal duration_Min = BigDecimal.valueOf(elapsedMillis).divide(BigDecimal.valueOf(60_000),2,RoundingMode.HALF_UP);

        // 8. 다음 공정 명 확인 ( 일단 벌크검사까지는 자동적으로 넘어갈 예쩡)
        String nextProcessCode = getNextProcessCode(entity.getProcess_code());

        // 벌크 검사 -> 최종 포장 검사는 합격 확인기능 연결 후 진행
        boolean startNext = nextProcessCode != null && ! "벌크 검사".equals(entity.getProcess_code());
        // 9. 자동 시작할 공정이 이미 진행 중인지 확인
        if(startNext){
            long nextCount = pr.countByBatchAndProcessAndStatus(batchId, nextProcessCode, "진행중");
            if(nextCount > 0){throw new ResponseStatusException(HttpStatus.CONFLICT,"다음 공정이 이미 진행 중입니다. 실행 이력을 확인해 주세요.");}
        }

        // 10. 현재 공정 종료
        entity.setEnd_time(endTime);
        entity.setDuration_min(duration_Min);
        entity.setStatus("완료");
        Process_execution_Entity finished = pr.save(entity);

        // 11. 다음 고정 시작 (자동)
        Process_execution_Dto nextExecution = null;
        if(startNext){
            Process_execution_Entity nextEntity = Process_execution_Entity.builder()
                                        .batchesEntity(batch).process_code(nextProcessCode)
                                        .start_time(endTime).status("진행중").record_source("자동진행").build();
            Process_execution_Entity nextSaved = pr.save(nextEntity);
            nextExecution = Process_execution_Dto.from(nextSaved);
        }

        return Process_execution_Finish_Dto.builder().finishedExecution(Process_execution_Dto.from(finished))
                                            .nextExecution(nextExecution).build();

        

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


}