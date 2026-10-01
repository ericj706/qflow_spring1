package springproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.entity.Anomaly_event_Entity;
import springproject.model.repository.Anomaly_event_Repository;

@Service @RequiredArgsConstructor 

public class Anomaly_event_Service {
    private final Anomaly_event_Repository anomaly_event_Repository;

    // 1) 개별조회
    public Anomaly_event_Dto findOne( Long anomaly_id){
        Optional<Anomaly_event_Entity> optional = anomaly_event_Repository.findById(anomaly_id);
        if (optional.isPresent()) {
            Anomaly_event_Entity entity = optional.get();
            return Anomaly_event_Dto.from(entity);
        }return null;
    }
    
    // 2) 전체조회
    public List<Anomaly_event_Dto> findAll(){
        List<Anomaly_event_Entity> anomaly_event_Entities = anomaly_event_Repository.findAll();
        List<Anomaly_event_Dto> dtos = new ArrayList<>();
        for(Anomaly_event_Entity entity : anomaly_event_Entities){
            Anomaly_event_Dto dto = Anomaly_event_Dto.from(entity);
            dtos.add(dto);
        }
        return dtos;
    }

}
