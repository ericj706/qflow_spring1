package springproject.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.dto.search.Bulk_qc_SearchDto;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.repository.Bulk_qc_Repository;

@Service 
@RequiredArgsConstructor 
public class Bulk_qc_Service {
    private final Bulk_qc_Repository bulk_qc_Repository;

    // 전체 조회 + 조건검색 + 페이징
    public List<Bulk_qc_Dto> search(Bulk_qc_SearchDto searchDto){
        LocalDate startDate = searchDto.getStarDate();
        LocalDate endDate = searchDto.getEndDate();
        // 기간 확인
        if(startDate != null && endDate != null && startDate.isAfter(endDate)){throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다");}
        
        // 담당잠 번호 확인 userId
        if(searchDto.getUserId() != null && searchDto.getUserId() <= 0){throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자 번호는 1 이상");}

        // 빈 문자열 검색조건에서 제외
        String batchId = searchDto.getBatchId();
        if(batchId != null){batchId = batchId.trim();
            if(batchId.isEmpty()){batchId=null;}} // 양끝 공백 제거

        // 검사 결과 , 공백 제거 앞뒤, 빈 문자열 조건 제외
        String overallQcResult = searchDto.getOverallQcResult();
        if(overallQcResult != null){overallQcResult = overallQcResult.trim();
            if(overallQcResult.isEmpty()){overallQcResult=null;}}

        // 검색시작일 00:00 이상
        LocalDateTime startAt = null;
        if(startDate != null){startAt = startDate.atStartOfDay();}

        // 검색 종요일 다음 날 00:00 미만  ( 종료일 당일의 모든 시간을 포함하기 위한 처리)
        LocalDateTime endAtExclusive = null;
        if(endDate != null){endAtExclusive = endDate.plusDays(1).atStartOfDay();}

        // 조건에 맞는 전체 기록 조회
        Integer userId = searchDto.getUserId();
        List<Bulk_qc_Entity> entities = bulk_qc_Repository.search(startAt, endAtExclusive, batchId, overallQcResult, userId);
        
        return  entities.stream().map(Bulk_qc_Dto::from).toList();
        
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
    
}
