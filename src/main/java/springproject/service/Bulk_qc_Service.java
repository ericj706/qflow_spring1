package springproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Bulk_qc_Dto;
import springproject.model.entity.Bulk_qc_Entity;
import springproject.model.repository.Bulk_qc_Repository;

@Service 
@RequiredArgsConstructor 
public class Bulk_qc_Service {
    private final Bulk_qc_Repository bulk_qc_Repository;

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
    
}
