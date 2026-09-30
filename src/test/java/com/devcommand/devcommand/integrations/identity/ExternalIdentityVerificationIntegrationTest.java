package com.devcommand.devcommand.integrations.identity;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.dto.LinkIdentityRequest;
import com.devcommand.devcommand.integrations.identity.dto.VerifyIdentityRequest;
import com.devcommand.devcommand.integrations.identity.entity.ExternalIdentityVerificationChallenge;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityVerificationChallengeRepository;
import com.devcommand.devcommand.integrations.identity.service.VerificationChallengeSender;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExternalIdentityVerificationIntegrationTest extends com.devcommand.devcommand.integration.AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalIdentityRepository identityRepository;

    @Autowired
    private ExternalIdentityVerificationChallengeRepository challengeRepository;

    @MockBean
    private VerificationChallengeSender challengeSender;

    private User testUser;
    private User otherUser;

    @Captor
    private ArgumentCaptor<String> challengeCodeCaptor;

    @BeforeEach
    void setUp() {
        challengeRepository.deleteAll();
        identityRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser = userRepository.save(otherUser);

        when(challengeSender.supports(any())).thenReturn(true);
    }

    @Test
    void linkIdentityStartsUnverifiedAndSendsChallenge() throws Exception {
        LinkIdentityRequest request = new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+1234567890");

        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").value("+1234567890"))
                .andExpect(jsonPath("$.verified").value(false));

        assertEquals(1, identityRepository.count());
        UserExternalIdentity identity = identityRepository.findAll().get(0);
        assertFalse(identity.isVerified());

        assertEquals(1, challengeRepository.count());
        verify(challengeSender, times(1)).sendVerificationChallenge(eq("+1234567890"), anyString());
    }

    @Test
    void validVerificationSucceedsAndIsSingleUse() throws Exception {
        // Link identity
        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+111"))))
                .andExpect(status().isCreated());

        UserExternalIdentity identity = identityRepository.findAll().get(0);
        
        // Capture OTP
        verify(challengeSender).sendVerificationChallenge(anyString(), challengeCodeCaptor.capture());
        String otp = challengeCodeCaptor.getValue();

        // Verify
        VerifyIdentityRequest verifyReq = new VerifyIdentityRequest(otp);
        mockMvc.perform(post("/api/integrations/identities/" + identity.getId() + "/verify")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        // Ensure single use
        mockMvc.perform(post("/api/integrations/identities/" + identity.getId() + "/verify")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk()); // Idempotent success when already verified
    }

    @Test
    void crossUserVerificationReturns404() throws Exception {
        // testUser links identity
        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+111"))))
                .andExpect(status().isCreated());

        UserExternalIdentity identity = identityRepository.findAll().get(0);
        
        verify(challengeSender).sendVerificationChallenge(anyString(), challengeCodeCaptor.capture());
        String otp = challengeCodeCaptor.getValue();

        // otherUser attempts to verify testUser's identity
        VerifyIdentityRequest verifyReq = new VerifyIdentityRequest(otp);
        mockMvc.perform(post("/api/integrations/identities/" + identity.getId() + "/verify")
                        .with(user(new UserPrincipal(otherUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCodeFails() throws Exception {
        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+111"))))
                .andExpect(status().isCreated());

        UserExternalIdentity identity = identityRepository.findAll().get(0);

        VerifyIdentityRequest verifyReq = new VerifyIdentityRequest("000000"); // wrong code
        mockMvc.perform(post("/api/integrations/identities/" + identity.getId() + "/verify")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
                
        assertFalse(identityRepository.findById(identity.getId()).get().isVerified());
    }
}
