package com.devcommand.devcommand.command.identity;

import com.devcommand.devcommand.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExternalIdentityResolverTest {

    private InMemoryExternalIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryExternalIdentityResolver();
    }

    @Test
    void resolve_ReturnsUser_WhenIdentityRegistered() {
        ExternalIdentity identity = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, "+1234567890");
        User mockUser = new User();
        mockUser.setId(99L);

        resolver.register(identity, mockUser);

        Optional<User> resolved = resolver.resolve(identity);
        assertTrue(resolved.isPresent());
        assertEquals(99L, resolved.get().getId());
    }

    @Test
    void resolve_ReturnsEmpty_WhenIdentityUnknown() {
        ExternalIdentity identity = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, "+0000000000");
        assertTrue(resolver.resolve(identity).isEmpty());
    }

    @Test
    void providerIsPartOfIdentityMatching() {
        ExternalIdentity whatsappIdentity = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, "123");
        ExternalIdentity githubIdentity = new ExternalIdentity(ExternalIdentityProvider.GITHUB, "123");
        
        User mockUser = new User();
        mockUser.setId(99L);

        resolver.register(whatsappIdentity, mockUser);

        assertTrue(resolver.resolve(whatsappIdentity).isPresent());
        assertTrue(resolver.resolve(githubIdentity).isEmpty());
    }
}
