package ru.aston.notificationservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.aston.notificationservice.event.UserEvent;
import ru.aston.notificationservice.service.EmailService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void consume(UserEvent event) {
        log.info("Received event: {}", event);
        String email = event.getEmail();
        if ("CREATE".equals(event.getOperation())) {
            emailService.sendEmail(email,
                    "Account Created",
                    "Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
        } else if ("DELETE".equals(event.getOperation())) {
            emailService.sendEmail(email,
                    "Account Deleted",
                    "Здравствуйте! Ваш аккаунт был удалён.");
        }
    }
}