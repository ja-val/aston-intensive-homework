package ru.aston.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationControllerIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendEmail_ShouldDeliverMessage() throws Exception {
        var request = new EmailRequest("user@example.com", "Test Subject", "Hello, World!");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmailRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/notifications/send", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(greenMail.getReceivedMessages()).hasSize(1);

        var mimeMessage = greenMail.getReceivedMessages()[0];
        assertThat(mimeMessage.getSubject()).isEqualTo("Test Subject");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
    }

    @lombok.Value
    private static class EmailRequest {
        String to;
        String subject;
        String text;
    }
}