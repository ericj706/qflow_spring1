package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Batches_Entity;

@Repository 
public interface  Batches_Repository extends JpaRepository<Batches_Entity, String> {
    
}
