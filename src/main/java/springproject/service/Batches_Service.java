package springproject.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Batches_Dto;
import springproject.model.repository.Batches_Repository;

@Service 
@RequiredArgsConstructor
public class Batches_Service {
    private final Batches_Repository br;
    
    // 전체조회( entity -> dto )
    @Transactional(readOnly = true)
    public List<Batches_Dto> findAll(){
        return br.findAll().stream()    
            .map(Batches_Dto::from)
            .toList();
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public Batches_Dto findOne(String batchId) {
        return br.findById(batchId)
                .map(Batches_Dto::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배치 ID"));
    }
}