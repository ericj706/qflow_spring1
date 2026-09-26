package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Users_Entity;

@Repository 
public interface Users_Repository extends JpaRepository<Users_Entity, Integer> {
    
}
