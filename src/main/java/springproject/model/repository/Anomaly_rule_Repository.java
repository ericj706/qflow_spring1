package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Anomaly_rule_Entity;

@Repository 
public interface Anomaly_rule_Repository extends JpaRepository<Anomaly_rule_Entity, Integer> {
    
}
