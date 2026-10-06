package springproject.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.dto.search.Process_execution_SearchDto;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.repository.Process_execution_Repository;

@Service
@RequiredArgsConstructor

public class Process_execution_Service {

    private final Process_execution_Repository pr;

    // 전체조회 : Entity 목록을 조회한 후 DTO 목록으로 변환
     public List<Process_execution_Dto> findAll() {

        List<Process_execution_Entity> entityList = pr.findAll();
        List<Process_execution_Dto> dtoList = new ArrayList<>();

        for (Process_execution_Entity entity : entityList) {
            dtoList.add(Process_execution_Dto.from(entity));
        }

        return dtoList;
    }

    // 전체조회 + 조건검색 + 페이징
     @Transactional (readOnly = true)
    public Page<Process_execution_Dto> search(
            Process_execution_SearchDto searchDto,
            Pageable pageable) {

        // 시작일
        LocalDateTime startAt = null;

        if (searchDto.getStartDate() != null) {
            startAt = searchDto.getStartDate().atStartOfDay();
        }


        // 종료일
        // 종료일 다음날 00:00 미만으로 검색해서
        // 종료일 하루 전체가 포함되도록 처리
        LocalDateTime endAtExclusive = null;

        if (searchDto.getEndDate() != null) {
            endAtExclusive = searchDto
                    .getEndDate()
                    .plusDays(1)
                    .atStartOfDay();
        }


        // Repository 조건검색 + 페이징
        return pr.search(
                startAt,
                endAtExclusive,
                searchDto.getBatchId(),
                searchDto.getProcessCode(),
                searchDto.getStatus(),
                pageable
        ).map(Process_execution_Dto::from);
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