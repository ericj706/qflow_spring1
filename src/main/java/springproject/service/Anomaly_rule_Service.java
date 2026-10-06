package springproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import springproject.model.dto.Anomaly_rule_Dto;
import springproject.model.entity.Anomaly_rule_Entity;
import springproject.model.repository.Anomaly_rule_Repository;

@Service @RequiredArgsConstructor 
@Transactional(readOnly = true)
public class Anomaly_rule_Service {
    private final Anomaly_rule_Repository anomaly_rule_Repository;

    // 1) 개별조회
    public Anomaly_rule_Dto findOne( Integer rule_id){
        Optional<Anomaly_rule_Entity> optional = anomaly_rule_Repository.findById(rule_id);
        if (optional.isPresent()) {
            Anomaly_rule_Entity anomaly_rule_Entity = optional.get();
            return Anomaly_rule_Dto.from(anomaly_rule_Entity);
        }return null;
    }
    
    // 2) 전체조회
    public List<Anomaly_rule_Dto> findAll(){
        List<Anomaly_rule_Entity> anomaly_rule_Entities = 
        anomaly_rule_Repository.findAllByOrderByRuleIdDesc();
        List<Anomaly_rule_Dto> dtos = new ArrayList<>();
        for(Anomaly_rule_Entity entity : anomaly_rule_Entities){
            Anomaly_rule_Dto dto = Anomaly_rule_Dto.from(entity);
            dtos.add(dto);
        }
        return dtos;
    }
    
    // 3) 현재 사용중인 이상규칙만 조회 (is_active = true)
    @Transactional (readOnly = true)
    public List<Anomaly_rule_Dto> findCurrRule() {
        List<Anomaly_rule_Entity> entities = anomaly_rule_Repository.findByIsActiveTrueOrderByRuleIdAsc();
        List<Anomaly_rule_Dto> dtos = new ArrayList<>();
        for (Anomaly_rule_Entity entity : entities) {
            dtos.add(Anomaly_rule_Dto.from(entity));
        }
        return dtos;
    }

    // 4) 신규 이상규칙 저장(수정)
    @Transactional 
    public Anomaly_rule_Dto saveNewRule(Anomaly_rule_Dto dto){
        // 동일공정,동일검사항목 -> 현재 사용중인 규칙 조회
        Optional<Anomaly_rule_Entity> opt = anomaly_rule_Repository
            .findByProcessCodeAndSensorNameAndIsActiveTrue(dto.getProcessCode(), dto.getSensorName());
        // 이전규칙이 존재하면 isActive->false
        opt.ifPresent(oldRule -> {
            oldRule.setIsActive(false);
        });
        // 새로 개정된 규칙 isActive->true
        Anomaly_rule_Entity newEntity = dto.toEntity();
        newEntity.setIsActive(true);

        Anomaly_rule_Entity saved = anomaly_rule_Repository.save(newEntity);
        return Anomaly_rule_Dto.from(saved);
    }

    // 5) 사용여부 수정 (토글스위치)
    @Transactional 
    public Anomaly_rule_Dto activeChange(Integer ruleId){
        Anomaly_rule_Entity entity = anomaly_rule_Repository.findById(ruleId)
            .orElseThrow( ()-> new IllegalArgumentException("존재하지 않는 규칙:"+ruleId));
        if (Boolean.TRUE.equals(entity.getIsActive())) {
            throw new RuntimeException("작업자의 동의 필요");
        }
        Optional<Anomaly_rule_Entity> currentStatus = anomaly_rule_Repository
            .findByProcessCodeAndSensorNameAndIsActiveTrue(entity.getProcessCode(), entity.getSensorName());
        currentStatus.ifPresent(oldRule -> {
            oldRule.setIsActive(false);
        });
        entity.setIsActive(true);
        Anomaly_rule_Entity updated = anomaly_rule_Repository.save(entity);
        return Anomaly_rule_Dto.from(updated);
    }
    
}
