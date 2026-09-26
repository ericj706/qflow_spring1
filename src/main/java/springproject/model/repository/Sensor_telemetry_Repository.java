package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Sensor_telemetry_Entity;

@Repository 
public interface Sensor_telemetry_Repository extends JpaRepository<Sensor_telemetry_Entity, Long> {
    
}
