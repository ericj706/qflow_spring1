package springproject.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.model.dto.Material_dispensing_Dto;
import springproject.model.repository.Material_dispensing_Repository;

@Service 
@RequiredArgsConstructor 
public class Material_dispensing_Service {
    private final Material_dispensing_Repository mr;

    // 전체조회( entity -> dto )
    @Transactional(readOnly = true)
    public List<Material_dispensing_Dto> findAll(){
        return mr.findAll().stream()    
            .map(Material_dispensing_Dto::from)
            .toList();
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public Material_dispensing_Dto findOne(String dispenseId) {
        return mr.findById(dispenseId)
                .map(Material_dispensing_Dto::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ID"));
    }
}
