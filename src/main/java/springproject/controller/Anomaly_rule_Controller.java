package springproject.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import springproject.service.Anomaly_rule_Service;

@RestController @RequiredArgsConstructor 
@RequestMapping ("/api/anomaly_rules")
public class Anomaly_rule_Controller {
    private final Anomaly_rule_Service anomaly_rule_Service;

    // 1) 개별조회
    @GetMapping("/{rule_id}")
    

}
