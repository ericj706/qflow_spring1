package springproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Filling_packaging_Dto;
import springproject.model.entity.Filling_packaging_Entity;
import springproject.model.repository.Filling_packaging_Repository;

@Service 
@RequiredArgsConstructor 
public class Filling_packaging_Service {
    private final Filling_packaging_Repository filling_packaging_Repository;

    // 전체조회
    public List<Filling_packaging_Dto> findAll(){
        List<Filling_packaging_Entity> filling_packaging_Entities = filling_packaging_Repository.findAll();
        List<Filling_packaging_Dto> dtos = new ArrayList<>();

        for(Filling_packaging_Entity entity: filling_packaging_Entities){
            Filling_packaging_Dto dto= Filling_packaging_Dto.from(entity);
            dtos.add(dto);
        }
        return  dtos;
    }

    // 개별조회
    public Filling_packaging_Dto findOne(String pouch_id){
        Optional<Filling_packaging_Entity> optional = filling_packaging_Repository.findById(pouch_id);
        if(optional.isPresent()){
            Filling_packaging_Entity entity = optional.get();
            return Filling_packaging_Dto.from(entity);
        }
        return null;
    }
    
}
