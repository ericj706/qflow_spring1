package springproject.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_rule_Dto;
import springproject.service.Anomaly_rule_Service;

@RestController @RequiredArgsConstructor 
@RequestMapping ("/api/anomaly-rules")
public class Anomaly_rule_Controller {
    private final Anomaly_rule_Service anomaly_rule_Service;

    // 1) 개별조회
    @GetMapping ("/{rule_id}")
    public Anomaly_rule_Dto findOne(@PathVariable (name = "rule_id") Integer rule_id){
        return anomaly_rule_Service.findOne(rule_id);
    }
    
    // 2) 전체조회
    @GetMapping ("")
    public List<Anomaly_rule_Dto> findAll(){
        return anomaly_rule_Service.findAll();
    }

}
