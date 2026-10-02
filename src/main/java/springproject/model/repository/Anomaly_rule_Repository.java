package springproject.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Anomaly_rule_Entity;

@Repository 
public interface Anomaly_rule_Repository extends JpaRepository<Anomaly_rule_Entity, Integer> {
    // 전체 이력 목록(정렬조건)
    List<Anomaly_rule_Entity> findAllByOrderByRuleIdDesc();
    // 현재 사용중인 이상감지규칙만 조회
    List<Anomaly_rule_Entity> findByIsActiveTrueOrderByRuleIdAsc();
    // 동일공정,동일센서항목인것 조회, 상태값 변경
    Optional<Anomaly_rule_Entity> findByProcessCodeAndSensorNameAndIsActiveTrue(String processCode, String sensorName);
}
