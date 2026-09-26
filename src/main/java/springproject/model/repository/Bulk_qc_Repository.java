package springproject.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Bulk_qc_Entity;

@Repository 
public interface Bulk_qc_Repository extends JpaRepository<Bulk_qc_Entity, String> {
    
}
