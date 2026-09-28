package springproject.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.repository.Process_execution_Repository;

// findAll() -> Repository.findAll()-> SELECT 전체 -> findOne(3) -> Repository.findById(3) -> execution_id = 3인 데이터

@Service 
@RequiredArgsConstructor 
public class Process_execution_Service {
    private final Process_execution_Repository pr;

    // 전체조회
    public List<Process_execution_Entity>findAll(){
        return pr.findAll();
    }

    // PK로 개별 조회
    public Process_execution_Entity findOne(Long execution_id){
        return pr.findById(execution_id).orElse(null);
    }
}
