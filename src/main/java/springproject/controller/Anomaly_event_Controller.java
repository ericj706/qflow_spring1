package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.service.Anomaly_event_Service;

@CrossOrigin (origins = "http://localhost:5173")
@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/anomaly-events")
public class Anomaly_event_Controller {
    private final Anomaly_event_Service anomaly_event_Service;

    // 2) 전체조회 + 조건검색 + 페이징  // 조건이 없으면 전체 목록 반환
        @GetMapping ("")
        public Page_response<Anomaly_event_Dto> findaAll(@ModelAttribute  Anomaly_event_SearchDto searchDto, @RequestParam(name = "page", defaultValue = "0") int page ){
            Page<Anomaly_event_Dto> result  = anomaly_event_Service.search(searchDto, page);
            return  Page_response.from(result, Map.of());
        }
    // 1) 개별조회
    @GetMapping ("/{anomaly_id}")
    public Anomaly_event_Dto findOne(@PathVariable (name = "anomaly_id") Long anomaly_id){
        return anomaly_event_Service.findOne(anomaly_id);
    }
    
    
}
