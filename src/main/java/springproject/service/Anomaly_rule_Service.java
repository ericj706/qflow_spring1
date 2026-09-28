package springproject.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import springproject.model.repository.Anomaly_rule_Repository;

@Service @RequiredArgsConstructor 
@RequestMapping ("/api/anomaly_rules")
public class Anomaly_rule_Service {
    private final Anomaly_rule_Repository anomaly_rule_Repository;

    // 1) 전체조회
    
    
    
}
