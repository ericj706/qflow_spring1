package springproject.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_rule_Dto;
import springproject.model.entity.Anomaly_rule_Entity;
import springproject.model.repository.Anomaly_rule_Repository;

@Service @RequiredArgsConstructor 

public class Anomaly_rule_Service {
    private final Anomaly_rule_Repository anomaly_rule_Repository;

    // 1) 개별조회
    public Anomaly_rule_Dto getFindAll( Integer ruleId){
        Optional<Anomaly_rule_Entity> optional = anomaly_rule_Repository.findById(ruleId);
        if (optional.isPresent()) {
            Anomaly_rule_Entity anomaly_rule_Entity = optional.get();
            return Anomaly_rule_Dto.from(anomaly_rule_Entity);
        }return null;
    }
    // 2) 전체조회


}
