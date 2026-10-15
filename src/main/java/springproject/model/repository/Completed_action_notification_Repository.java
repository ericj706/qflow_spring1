package springproject.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springproject.model.entity.Completed_action_notification_Entity;

@Repository
public interface Completed_action_notification_Repository
        extends JpaRepository<Completed_action_notification_Entity, Long> {

    // 특정 최고관리자의 읽지 않은 알림 조회
    List<Completed_action_notification_Entity>
        findByRecipientUser_UserIdAndIsReadFalseOrderByCreatedAtAsc(
            Integer userId
        );

    // 동일한 이상번호에 대한 중복 알림 생성 방지
    boolean existsByAnomalyEvent_AnomalyIdAndRecipientUser_UserId(
        Long anomalyId,
        Integer userId
    );
}