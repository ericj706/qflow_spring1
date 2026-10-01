package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.service.Anomaly_event_Service;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/anomaly-events")
public class Anomaly_event_Controller {
    private final Anomaly_event_Service anomaly_event_Service;

    // 2) 전체조회  // 조건이 없으면 전체 목록 반환
        @GetMapping ("")
        public List<Anomaly_event_Dto> findAll( @ModelAttribute Anomaly_event_SearchDto searchDto){
            return anomaly_event_Service.search(searchDto);
        }
    // 1) 개별조회
    @GetMapping ("/{anomaly_id}")
    public Anomaly_event_Dto findOne(@PathVariable (name = "anomaly_id") Long anomaly_id){
        return anomaly_event_Service.findOne(anomaly_id);
    }
    
    
}
