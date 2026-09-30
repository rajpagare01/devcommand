package com.devcommand.devcommand.integrations.whatsapp.repository;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.whatsapp.entity.ExternalWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalWebhookEventRepository extends JpaRepository<ExternalWebhookEvent, Long> {
    
    Optional<ExternalWebhookEvent> findByProviderAndExternalEventId(ExternalIdentityProvider provider, String externalEventId);
}
