package springproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Data_change_log_Dto;
import springproject.model.entity.Data_change_log_Entity;
import springproject.model.repository.Data_change_log_Repository;

@Service @RequiredArgsConstructor 
public class Data_change_log_Service {
    private final Data_change_log_Repository data_change_log_Repository;
    // 1) 개별조회
    public Data_change_log_Dto findOne(Long change_id){
        Optional<Data_change_log_Entity> optional = data_change_log_Repository.findById(change_id);
        if(optional.isPresent()){
            Data_change_log_Entity entity = optional.get();
            return Data_change_log_Dto.from(entity);
        }
        return null;
    }
    // 2) 전체조회
    public List<Data_change_log_Dto> findAll(){
        List<Data_change_log_Entity> data_change_log_Entities = data_change_log_Repository.findAll();
        List<Data_change_log_Dto> dtos = new ArrayList<>();

        for(Data_change_log_Entity entity: data_change_log_Entities){
            Data_change_log_Dto dto= Data_change_log_Dto.from(entity);
            dtos.add(dto);
        }
        return dtos;
    }


}
