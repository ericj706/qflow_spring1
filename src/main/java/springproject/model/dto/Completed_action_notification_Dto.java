package springproject.model.dto;

public record Completed_action_notification_Dto(
        Long notificationId,
        Anomaly_event_Dto anomaly
) {
}