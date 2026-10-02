package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_rule_Dto;
import springproject.service.Anomaly_rule_Service;

@CrossOrigin (origins = "http://localhost:5173",
    allowedHeaders = "*", 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    maxAge = 3600)
@RestController @RequiredArgsConstructor 
@RequestMapping ("/mask/anomaly-rules")
public class Anomaly_rule_Controller {
    private final Anomaly_rule_Service anomaly_rule_Service;

    // 1) 개별조회
    @GetMapping ("/{rule_id}")
    public Anomaly_rule_Dto findOne(@PathVariable (name = "rule_id") Integer rule_id){
        return anomaly_rule_Service.findOne(rule_id);
    }
    // 2) 전체조회
    @GetMapping ("")
    public List<Anomaly_rule_Dto> findAll(
        @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return anomaly_rule_Service.findCurrRule();
        }
        return anomaly_rule_Service.findAll();
    }

    // 3) 신규 이상규칙 저장
    @PostMapping("")
    public Anomaly_rule_Dto saveNewRule(@RequestBody Anomaly_rule_Dto dto){
        return anomaly_rule_Service.saveNewRule(dto);
    }

    // 4) 사용여부 수정
    @PutMapping ("/{ruleId}/change")
    public Anomaly_rule_Dto activeChange(@PathVariable ("ruleId") Integer ruleId){
        return anomaly_rule_Service.activeChange(ruleId);
    }

}
