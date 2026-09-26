package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Anomaly_event_Entity;

@Repository 
public interface Anomaly_event_Repository extends JpaRepository<Anomaly_event_Entity, Long> {
    
}
