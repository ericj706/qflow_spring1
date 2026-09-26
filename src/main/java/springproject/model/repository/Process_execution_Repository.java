package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Process_execution_Entity;

@Repository 
public interface Process_execution_Repository extends JpaRepository<Process_execution_Entity, Long> {
    
}
