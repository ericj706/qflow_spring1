package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.service.Anomaly_event_Service;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/anomaly-events")
@CrossOrigin (origins = "http://localhost:5173/")
public class Anomaly_event_Controller {
    private final Anomaly_event_Service anomaly_event_Service;

    // 1) 개별조회
    @GetMapping ("/{anomaly_id}")
    public Anomaly_event_Dto findOne(@PathVariable (name = "anomaly_id") Long anomaly_id){
        return anomaly_event_Service.findOne(anomaly_id);
    }
    
    // 2) 전체조회
    @GetMapping ("")
    public List<Anomaly_event_Dto> findAll(){
        return anomaly_event_Service.findAll();
    }
}
