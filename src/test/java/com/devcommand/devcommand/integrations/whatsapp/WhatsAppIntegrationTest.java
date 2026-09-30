package com.devcommand.devcommand.integrations.whatsapp;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.integrations.whatsapp.entity.ExternalWebhookEvent;
import com.devcommand.devcommand.integrations.whatsapp.repository.ExternalWebhookEventRepository;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "twilio.account-sid=test-sid",
    "twilio.auth-token=test-token",
    "twilio.whatsapp-number=whatsapp:+12345"
})
@AutoConfigureMockMvc
public class WhatsAppIntegrationTest extends com.devcommand.devcommand.integration.AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalIdentityRepository identityRepository;

    @Autowired
    private ExternalWebhookEventRepository eventRepository;

    @Autowired
    private DailyTaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        eventRepository.deleteAll();
        identityRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("webhook_user");
        testUser.setEmail("webhook@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);
    }

    @Test
    void whenMissingSignature_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/integrations/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+5551234567")
                        .param("To", "whatsapp:+12345")
                        .param("Body", "add task: fix something")
                        .param("MessageSid", "SM123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenInvalidSignature_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/integrations/whatsapp/webhook")
                        .header("X-Twilio-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+5551234567")
                        .param("To", "whatsapp:+12345")
                        .param("Body", "add task: fix something")
                        .param("MessageSid", "SM123"))
                .andExpect(status().isForbidden());
    }

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.devcommand.devcommand.integrations.whatsapp.service.TwilioSignatureValidator signatureValidator;

    @Test
    void whenValidSignature_andUnverifiedIdentity_thenReturnsErrorXml() throws Exception {
        org.mockito.Mockito.when(signatureValidator.validate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        UserExternalIdentity identity = new UserExternalIdentity(testUser, ExternalIdentityProvider.WHATSAPP, "+5551234567", false);
        identityRepository.save(identity);

        mockMvc.perform(post("/api/integrations/whatsapp/webhook")
                        .header("X-Twilio-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+5551234567")
                        .param("To", "whatsapp:+12345")
                        .param("Body", "add task: fix something")
                        .param("MessageSid", "SM123"))
                .andExpect(status().isOk())
                .andExpect(content().xml("<Response><Message>Your WhatsApp number is not linked or verified.</Message></Response>"));
    }

    @Test
    void whenValidSignature_andVerifiedIdentity_andValidCommand_thenExecutesAndReturnsSuccess() throws Exception {
        org.mockito.Mockito.when(signatureValidator.validate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        UserExternalIdentity identity = new UserExternalIdentity(testUser, ExternalIdentityProvider.WHATSAPP, "+5551234567", true);
        identityRepository.save(identity);

        mockMvc.perform(post("/api/integrations/whatsapp/webhook")
                        .header("X-Twilio-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+5551234567")
                        .param("To", "whatsapp:+12345")
                        .param("Body", "add task: fix something")
                        .param("MessageSid", "SM456"))
                .andExpect(status().isOk())
                .andExpect(content().xml("<Response><Message>Task created successfully.</Message></Response>"));

        List<DailyTask> tasks = taskRepository.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("fix something");
        assertThat(tasks.get(0).getUser().getId()).isEqualTo(testUser.getId());

        List<ExternalWebhookEvent> events = eventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getExternalEventId()).isEqualTo("SM456");
        assertThat(events.get(0).getStatus()).isEqualTo(ExternalWebhookEvent.EventStatus.PROCESSED);
    }
    
    @Test
    void whenDuplicateWebhook_thenReturnsEmptyResponse() throws Exception {
        org.mockito.Mockito.when(signatureValidator.validate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        ExternalWebhookEvent event = new ExternalWebhookEvent(ExternalIdentityProvider.WHATSAPP, "SM789", ExternalWebhookEvent.EventStatus.PROCESSED);
        eventRepository.save(event);

        mockMvc.perform(post("/api/integrations/whatsapp/webhook")
                        .header("X-Twilio-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+5551234567")
                        .param("To", "whatsapp:+12345")
                        .param("Body", "add task: fix something")
                        .param("MessageSid", "SM789"))
                .andExpect(status().isOk())
                .andExpect(content().xml("<Response></Response>"));
                
        // Task should not be created
        List<DailyTask> tasks = taskRepository.findAll();
        assertThat(tasks).isEmpty();
    }
}
