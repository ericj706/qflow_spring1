package springproject.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_event_Dto;
import springproject.model.dto.page.Page_response;
import springproject.model.dto.request.Anomaly_event_Action_Dto;
import springproject.model.dto.request.Anomaly_event_Confirm_Dto;
import springproject.model.dto.search.Anomaly_event_SearchDto;
import springproject.service.Anomaly_event_Service;

@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/anomaly-events")
public class Anomaly_event_Controller {
    private final Anomaly_event_Service anomaly_event_Service;

    // 2) 전체조회 + 조건검색 + 페이징  // 조건이 없으면 전체 목록 반환
    @GetMapping ("")
    public Page_response<Anomaly_event_Dto> findaAll(
        @ModelAttribute  Anomaly_event_SearchDto searchDto,
        @RequestParam(name = "page", defaultValue = "0") int page ){
        Page<Anomaly_event_Dto> result  = anomaly_event_Service.search(searchDto, page);
        return  Page_response.from(result, Map.of());
    }

    // 이상 알림 팝업 전용 조회
    @GetMapping(value = "/popup", produces = "application/json")
    public List<Anomaly_event_Dto> getPopupAnomalies() {
        return anomaly_event_Service.findActivePopupAnomalies();
    }

    // 1) 개별조회
        @GetMapping ("/{anomaly_id}")
        public Anomaly_event_Dto findOne(@PathVariable (name = "anomaly_id") Long anomaly_id){
            return anomaly_event_Service.findOne(anomaly_id);
        }

    // 공정 실행별 알람 조회
    @GetMapping("/executions/{executionId}")
    public List<Anomaly_event_Dto> findByExecutionId(@PathVariable(name = "executionId") Long executionId) {
        return anomaly_event_Service.findByExecutionId(executionId);
    }
    
    // 주의 알람 확인 처리
    @PatchMapping ("/{anomalyId}/confirm")
    public Anomaly_event_Dto confirm(@PathVariable(name = "anomalyId") Long anomalyId, @RequestBody Anomaly_event_Confirm_Dto request){
        return anomaly_event_Service.confirm(anomalyId, request);
    }
    
    // 심각 알람 조치 완료
    @PatchMapping("/{anomalyId}/action")
    public Anomaly_event_Dto completeAction(
        @PathVariable(name = "anomalyId") Long anomalyId,
        @RequestBody Anomaly_event_Action_Dto request){
        return anomaly_event_Service.completeAction(anomalyId, request);
    }
}
