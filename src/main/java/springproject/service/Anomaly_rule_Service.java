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
    // 규칙 존재 검증
    private Anomaly_rule_Entity requireRule(Integer id) {
        return anomaly_rule_Repository.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "존재하지 않는 규칙"));
    }
    // 이상감지규칙 입력값 검증
    private void validate(Anomaly_rule_Dto dto) {
        if (dto.getProcessCode() == null || dto.getProcessCode().isBlank() || dto.getProcessCode().length() > 30
            || dto.getSensorName() == null || dto.getSensorName().isBlank() || dto.getSensorName().length() > 50
            || dto.getAnomalyType() == null || dto.getAnomalyType().isBlank() || dto.getAnomalyType().length() > 50
            || dto.getConditionType() == null || dto.getConditionType().isBlank() || dto.getConditionType().length() > 20
            || dto.getDurationSeconds() == null || dto.getDurationSeconds() < 0 || dto.getIsActive() == null
            || (dto.getStatusValue() != null && dto.getStatusValue().length() > 50)) {
            badRequest("필수 항목과 지속시간을 확인해 주세요.");
        }
        java.math.BigDecimal[] values = {dto.getWarningMin(), dto.getWarningMax(), dto.getCriticalMin(), dto.getCriticalMax()};
        for (java.math.BigDecimal value : values) {
            if (value != null && (value.stripTrailingZeros().scale() > 3 || value.abs().compareTo(new java.math.BigDecimal("1000000000")) >= 0)) {
                badRequest("기준값은 소수점 3자리까지, 절댓값 10억 미만이어야 합니다.");
            }
        }
        if ((dto.getWarningMin() != null && dto.getWarningMax() != null && dto.getWarningMin().compareTo(dto.getWarningMax()) > 0)
            || (dto.getCriticalMin() != null && dto.getCriticalMax() != null && dto.getCriticalMin().compareTo(dto.getCriticalMax()) > 0)
            || (dto.getCriticalMin() != null && dto.getWarningMin() != null && dto.getCriticalMin().compareTo(dto.getWarningMin()) > 0)
            || (dto.getWarningMax() != null && dto.getCriticalMax() != null && dto.getWarningMax().compareTo(dto.getCriticalMax()) > 0)) {
            badRequest("최소값은 최대값 이하여야 하며 주의 범위는 심각 기준 범위 안에 있어야 합니다.");
        }
    }
    // 검증 실패 응답(예외처리에 반복사용)
    private void badRequest(String message) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, message);
    }
    // 중복 활성화된 규칙 검증
    private void deactivateConflicts(Anomaly_rule_Entity entity) {
        anomaly_rule_Repository.findByProcessCodeAndSensorNameAndConditionTypeAndIsActiveTrue(
            entity.getProcessCode(), entity.getSensorName(), entity.getConditionType())
            .stream().filter(old -> !old.getRuleId().equals(entity.getRuleId()))
            .forEach(old -> old.setIsActive(false));
    }
    // 신규 이상규칙 저장
    @Transactional
    public Anomaly_rule_Dto saveNewRule(Anomaly_rule_Dto dto) {
        validate(dto);
        Anomaly_rule_Entity entity = dto.toEntity();
        if (Boolean.TRUE.equals(entity.getIsActive())) { deactivateConflicts(entity); }
        return Anomaly_rule_Dto.from(anomaly_rule_Repository.save(entity));
    }

    // 기존 ID, 생성시간, 이상 발생 기록과의 연결 유지
    @Transactional
    public Anomaly_rule_Dto update(Integer ruleId, Anomaly_rule_Dto dto) {
        validate(dto);
        Anomaly_rule_Entity entity = requireRule(ruleId);
        entity.setProcessCode(dto.getProcessCode());
        entity.setSensorName(dto.getSensorName());
        entity.setAnomalyType(dto.getAnomalyType());
        entity.setConditionType(dto.getConditionType());
        entity.setWarningMin(dto.getWarningMin());
        entity.setWarningMax(dto.getWarningMax());
        entity.setCriticalMin(dto.getCriticalMin());
        entity.setCriticalMax(dto.getCriticalMax());
        entity.setStatusValue(dto.getStatusValue());
        entity.setDurationSeconds(dto.getDurationSeconds());
        entity.setCheckItems(dto.getCheckItems());
        entity.setResponseDescription(dto.getResponseDescription());
        entity.setIsActive(dto.getIsActive());
        if (Boolean.TRUE.equals(entity.getIsActive())) { deactivateConflicts(entity); }
        return Anomaly_rule_Dto.from(anomaly_rule_Repository.save(entity));
    }
    // 사용여부 전환
    @Transactional
    public Anomaly_rule_Dto activeChange(Integer ruleId) {
        Anomaly_rule_Entity entity = requireRule(ruleId);
        entity.setIsActive(!Boolean.TRUE.equals(entity.getIsActive()));
        if (Boolean.TRUE.equals(entity.getIsActive())) { deactivateConflicts(entity); }
        return Anomaly_rule_Dto.from(anomaly_rule_Repository.save(entity));
    }
}
