package springproject.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "completed_action_notification",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"anomaly_id", "recipient_user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Completed_action_notification_Entity {

    // 알림 고유번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    // 조치완료된 이상 알람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id", nullable = false)
    private Anomaly_event_Entity anomalyEvent;

    // 알림을 받는 최고관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private Users_Entity recipientUser;

    // 읽음 여부
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // 읽은 시간
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 알림 생성 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}