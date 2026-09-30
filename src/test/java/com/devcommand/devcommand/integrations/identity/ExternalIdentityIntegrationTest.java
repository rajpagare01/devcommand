package com.devcommand.devcommand.integrations.identity;

import com.devcommand.devcommand.command.identity.ExternalIdentity;
import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.command.identity.ExternalIdentityResolver;
import com.devcommand.devcommand.integrations.identity.dto.LinkIdentityRequest;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.devcommand.devcommand.security.UserPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExternalIdentityIntegrationTest extends com.devcommand.devcommand.integration.AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalIdentityRepository identityRepository;

    @Autowired
    private ExternalIdentityResolver externalIdentityResolver;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.devcommand.devcommand.integrations.identity.service.VerificationChallengeSender challengeSender;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        identityRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setName("User One");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("User Two");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2 = userRepository.save(user2);
        
        org.mockito.Mockito.when(challengeSender.supports(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void createIdentity_Success() throws Exception {
        LinkIdentityRequest request = new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+1234567890");

        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").value("+1234567890"))
                .andExpect(jsonPath("$.provider").value("WHATSAPP"));

        assertEquals(1, identityRepository.count());
    }

    @Test
    void createDuplicateIdentity_Fails() throws Exception {
        LinkIdentityRequest request = new LinkIdentityRequest(ExternalIdentityProvider.WHATSAPP, "+1234567890");

        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listIdentities_Success() throws Exception {
        UserExternalIdentity identity = new UserExternalIdentity(user1, ExternalIdentityProvider.WHATSAPP, "+111", true);
        identityRepository.save(identity);

        mockMvc.perform(get("/api/integrations/identities").with(user(new UserPrincipal(user1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].externalId").value("+111"));
    }

    @Test
    void deleteOwnIdentity_Success() throws Exception {
        UserExternalIdentity identity = new UserExternalIdentity(user1, ExternalIdentityProvider.WHATSAPP, "+111", true);
        identity = identityRepository.save(identity);

        mockMvc.perform(delete("/api/integrations/identities/" + identity.getId()).with(user(new UserPrincipal(user1))))
                .andExpect(status().isNoContent());

        assertEquals(0, identityRepository.count());
    }

    @Test
    void crossUserDelete_Rejected() throws Exception {
        UserExternalIdentity identity = new UserExternalIdentity(user2, ExternalIdentityProvider.WHATSAPP, "+222", true);
        identity = identityRepository.save(identity);

        mockMvc.perform(delete("/api/integrations/identities/" + identity.getId()).with(user(new UserPrincipal(user1))))
                .andExpect(status().isNotFound()); // The query is filtered by userId

        assertEquals(1, identityRepository.count());
    }

    @Test
    void resolverFindsCorrectUser_ProviderIsolation() {
        UserExternalIdentity identity1 = new UserExternalIdentity(user1, ExternalIdentityProvider.WHATSAPP, "+123", true);
        UserExternalIdentity identity2 = new UserExternalIdentity(user2, ExternalIdentityProvider.GITHUB, "+123", true);
        identityRepository.save(identity1);
        identityRepository.save(identity2);

        ExternalIdentity searchWhatsapp = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, "+123");
        Optional<User> resolved = externalIdentityResolver.resolve(searchWhatsapp);
        assertTrue(resolved.isPresent());
        assertEquals(user1.getId(), resolved.get().getId());

        ExternalIdentity searchGithub = new ExternalIdentity(ExternalIdentityProvider.GITHUB, "+123");
        Optional<User> resolved2 = externalIdentityResolver.resolve(searchGithub);
        assertTrue(resolved2.isPresent());
        assertEquals(user2.getId(), resolved2.get().getId());
    }

    @Test
    void resolverRejectsUnknownIdentity() {
        ExternalIdentity searchWhatsapp = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, "+000");
        Optional<User> resolved = externalIdentityResolver.resolve(searchWhatsapp);
        assertTrue(resolved.isEmpty());
    }

    @Test
    void missingProviderOrExternalId_FailsValidation() throws Exception {
        LinkIdentityRequest request = new LinkIdentityRequest(null, "");

        mockMvc.perform(post("/api/integrations/identities")
                        .with(user(new UserPrincipal(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseUniqueConstraintBehavior() {
        UserExternalIdentity identity1 = new UserExternalIdentity(user1, ExternalIdentityProvider.WHATSAPP, "+555", true);
        identityRepository.saveAndFlush(identity1);

        UserExternalIdentity identity2 = new UserExternalIdentity(user2, ExternalIdentityProvider.WHATSAPP, "+555", true);
        
        assertThrows(Exception.class, () -> identityRepository.saveAndFlush(identity2));
    }
}
