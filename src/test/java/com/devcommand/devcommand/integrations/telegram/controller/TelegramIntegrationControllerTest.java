package com.devcommand.devcommand.integrations.telegram.controller;

import com.devcommand.devcommand.integrations.telegram.service.TelegramBootstrapService;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class TelegramIntegrationControllerTest {

    @Test
    void generateBootstrapToken_usesAuthenticatedUserId() {
        TelegramBootstrapService bootstrapService = Mockito.mock(TelegramBootstrapService.class);
        TelegramIntegrationController controller = new TelegramIntegrationController(bootstrapService);

        User mockUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(mockUser, "id", 42L);
        UserPrincipal principal = new UserPrincipal(mockUser);

        when(bootstrapService.generateTokenForUser(42L)).thenReturn("mocked-token-123");

        ResponseEntity<Map<String, String>> response = controller.generateBootstrapToken(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("mocked-token-123", response.getBody().get("token"));
        Mockito.verify(bootstrapService).generateTokenForUser(42L);
    }
}
