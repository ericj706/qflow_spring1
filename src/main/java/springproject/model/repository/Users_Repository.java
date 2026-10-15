package springproject.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import springproject.model.entity.Users_Entity;

@Repository 
public interface Users_Repository extends JpaRepository<Users_Entity, Integer> {
    // 이름으로 유저 조회 메서드 -> 로그인 id 로 변경시 삭제
    Optional<Users_Entity> findByUserName(String userName);

    // 해당 역할을 가진 활성 회원을 사용자번호 순서로 조회
    List<Users_Entity> findByRoleAndIsActiveTrueOrderByUserIdAsc(String role);

    // 해당 공정의 활성 관리자를 사용자번호 순서로 조회하고 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM Users_Entity u
            WHERE u.role = :role
            AND u.isActive = true
            ORDER BY u.userId ASC
            """)
    List<Users_Entity> findManagersForUpdate(@Param("role") String role);
}
