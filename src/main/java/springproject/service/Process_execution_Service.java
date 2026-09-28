package springproject.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Process_execution_Dto;
import springproject.model.entity.Process_execution_Entity;
import springproject.model.repository.Process_execution_Repository;

@Service
@RequiredArgsConstructor
public class Process_execution_Service {

    private final Process_execution_Repository pr;

    // 전체조회 : Entity 목록을 조회한 후 DTO 목록으로 변환
    public List<Process_execution_Dto> findAll(){

        List<Process_execution_Entity> entityList = pr.findAll();
        List<Process_execution_Dto> dtoList = new ArrayList<>();

        for(Process_execution_Entity entity : entityList){
            dtoList.add(Process_execution_Dto.from(entity));
        }

        return dtoList;
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