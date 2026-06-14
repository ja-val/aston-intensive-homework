package ru.aston.notificationservice;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.aston.notificationservice.event.UserEvent;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"user-events"}
)
class UserEventConsumerIntegrationTest {

    private static final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);

    @DynamicPropertySource
    static void configureMail(DynamicPropertyRegistry registry) {
        greenMail.start();
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Test
    void consumeCreateEvent_SendsWelcomeEmail() throws Exception {
        UserEvent event = UserEvent.builder()
                .operation("CREATE")
                .email("newuser@example.com")
                .build();

        kafkaTemplate.send("user-events", event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(greenMail.getReceivedMessages()).hasSize(1)
        );

        var message = greenMail.getReceivedMessages()[0];
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("newuser@example.com");
        assertThat(message.getSubject()).contains("Account Created");
    }

    @Test
    void consumeDeleteEvent_SendsGoodbyeEmail() throws Exception {
        UserEvent event = UserEvent.builder()
                .operation("DELETE")
                .email("olduser@example.com")
                .build();

        kafkaTemplate.send("user-events", event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(greenMail.getReceivedMessages()).hasSize(1)
        );

        var message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("Account Deleted");
    }
}