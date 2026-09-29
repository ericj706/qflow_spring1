package springproject.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Sensor_telemetry_Dto;
import springproject.model.entity.Sensor_telemetry_Entity;
import springproject.model.repository.Sensor_telemetry_Repository;

@Service 
@RequiredArgsConstructor 
public class Sensor_telemetry_Service {
    
    private final Sensor_telemetry_Repository sr;

    // 전체조회 : Entity 목록을 조회한 후 DTO 목록으로 변환
    public List<Sensor_telemetry_Dto> findAll(){
        List<Sensor_telemetry_Entity> entityList = sr.findAll();
        List<Sensor_telemetry_Dto> dtoList = new ArrayList<>();

        // Entity -> DTO 변환
        for(Sensor_telemetry_Entity entity : entityList){
            dtoList.add(Sensor_telemetry_Dto.from(entity));
        }
        return dtoList;
    }

    // 개별조회 : PK(sensor_id)로 조회한 후 DTO 변환
    public Sensor_telemetry_Dto findOne(Long sensor_id){
        Sensor_telemetry_Entity entity =sr.findById(sensor_id).orElse(null);

        // 해당 PK의 데이터가 없으면 null 반환
        if(entity == null){
            return null;
        }
        return Sensor_telemetry_Dto.from(entity);
    }
}
