package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Data_change_log_Entity;

@Repository 
public interface Data_change_log_Repository extends JpaRepository<Data_change_log_Entity, Long> {
    
}
