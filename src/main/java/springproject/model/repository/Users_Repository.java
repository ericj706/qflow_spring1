package springproject.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Users_Entity;

@Repository 
public interface Users_Repository extends JpaRepository<Users_Entity, Integer> {
    // 이름으로 유저 조회 메서드 -> 로그인 id 로 변경시 삭제
    Optional<Users_Entity> findByUserName(String userName);
}
