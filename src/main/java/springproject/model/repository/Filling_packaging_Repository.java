package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Filling_packaging_Entity;

@Repository 
public interface Filling_packaging_Repository extends JpaRepository<Filling_packaging_Entity, String> {
    
}
