package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Material_dispensing_Entity;

@Repository 
public interface  Material_dispensing_Repository extends JpaRepository<Material_dispensing_Entity, String> {
    
}
