package springproject.service;

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
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.repository.Bulk_qc_Repository;

@Service 
@RequiredArgsConstructor 
public class Bulk_qc_Service {
    private final Bulk_qc_Repository bulk_qc_Repository;

    // 전체 조회 + 조건검색 + 페이징
    public Page<Bulk_qc_Dto> search(
        String batchId, Integer userId, int page, int size
    ){
        // 잘못된 페이지 요청 확인
        if (page < 0 || size <1 || size > 100){
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST,"페이지는 0이상, 조회 개수는 1~100이어야 합니다");
        }

        // 빈 문자열은 검색 조건에서 제외
        if(batchId != null){
            batchId = batchId.trim(); // 문자열 앞뒤 공백 제거
            if(batchId.isEmpty()){
                batchId = null;
            }
        }

        // 페이지 번호, 조회 개수, 정렬 설정
        Pageable pageable = PageRequest.of(
            page, 
            size, 
            Sort.by(
                Sort.Order.desc("Sample_time"), 
                Sort.Order.asc("qc_id")
            ) 
        );

        // Reposiotry에서 조회
        Page<Bulk_qc_Entity> entities = bulk_qc_Repository.search(batchId, userId, pageable);
        return  entities.map(Bulk_qc_Dto::from);
    }

    // 전체조회 
    public List<Bulk_qc_Dto> findAll(){
        List<Bulk_qc_Entity> bulk_qc_Entities = bulk_qc_Repository.findAll();
        List<Bulk_qc_Dto> bulk_qc_Dtos = new ArrayList<>();
        
        for(Bulk_qc_Entity entity: bulk_qc_Entities){
            bulk_qc_Dtos.add(Bulk_qc_Dto.from(entity));
        }
        return bulk_qc_Dtos;
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
