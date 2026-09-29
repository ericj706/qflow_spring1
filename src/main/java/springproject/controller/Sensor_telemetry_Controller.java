package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Sensor_telemetry_Dto;
import springproject.service.Sensor_telemetry_Service;

@RestController 
@RequestMapping("/mask/sensor-telemetries")
@RequiredArgsConstructor 
@CrossOrigin (origins = "http://localhost:5173")
public class Sensor_telemetry_Controller {
    private final Sensor_telemetry_Service ss;

    // 전체조회
    @GetMapping 
    public List<Sensor_telemetry_Dto>findAll(){
        return ss.findAll();
    }

    // PK로 개별조회
    @GetMapping("/findOne")
    public Sensor_telemetry_Dto findOne(Long sensor_id){
        return ss.findOne(sensor_id);
    }
}
