package org.dinhb.microservice.core.notification.web;

import org.dinhb.microservice.core.common.security.HeaderNames;
import org.dinhb.microservice.core.notification.domain.Notification;
import org.dinhb.microservice.core.notification.domain.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    public List<NotificationResponse> mine(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public record NotificationResponse(UUID id, UUID userId, String type, String message, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getUserId(), n.getType(), n.getMessage(), n.getCreatedAt());
        }
    }
}
