package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.identity.ExternalIdentity;
import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.command.identity.ExternalIdentityResolver;
import com.devcommand.devcommand.integrations.whatsapp.dto.WhatsAppMessage;
import com.devcommand.devcommand.integrations.whatsapp.entity.ExternalWebhookEvent;
import com.devcommand.devcommand.integrations.whatsapp.repository.ExternalWebhookEventRepository;
import com.devcommand.devcommand.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class WhatsAppWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);

    private final ExternalWebhookEventRepository eventRepository;
    private final ExternalIdentityResolver identityResolver;
    private final DeterministicCommandParser commandParser;
    private final CommandDispatcher commandDispatcher;

    public WhatsAppWebhookService(ExternalWebhookEventRepository eventRepository,
                                  ExternalIdentityResolver identityResolver,
                                  DeterministicCommandParser commandParser,
                                  CommandDispatcher commandDispatcher) {
        this.eventRepository = eventRepository;
        this.identityResolver = identityResolver;
        this.commandParser = commandParser;
        this.commandDispatcher = commandDispatcher;
    }

    /**
     * Entry point for Webhook processing. Handles idempotency.
     */
    @Transactional
    public String handleMessage(WhatsAppMessage message) {
        String eventId = message.messageSid();
        
        if (eventId == null || eventId.trim().isEmpty()) {
            log.warn("Received webhook without MessageSid");
            return "Internal error: Missing message ID";
        }

        Optional<ExternalWebhookEvent> existingEventOpt = eventRepository.findByProviderAndExternalEventId(
                ExternalIdentityProvider.WHATSAPP, eventId);

        if (existingEventOpt.isPresent()) {
            ExternalWebhookEvent existingEvent = existingEventOpt.get();
            if (existingEvent.getStatus() == ExternalWebhookEvent.EventStatus.PROCESSED) {
                log.info("Duplicate webhook received, already PROCESSED. EventID: {}", eventId);
                return ""; // Do not send a duplicate response
            } else if (existingEvent.getStatus() == ExternalWebhookEvent.EventStatus.PROCESSING) {
                log.info("Duplicate webhook received, currently PROCESSING. EventID: {}", eventId);
                return "";
            } else {
                log.info("Duplicate webhook received, previous status FAILED. Retrying. EventID: {}", eventId);
                existingEvent.setStatus(ExternalWebhookEvent.EventStatus.PROCESSING);
            }
        } else {
            ExternalWebhookEvent newEvent = new ExternalWebhookEvent(
                    ExternalIdentityProvider.WHATSAPP, 
                    eventId, 
                    ExternalWebhookEvent.EventStatus.PROCESSING
            );
            eventRepository.saveAndFlush(newEvent); // Flush to enforce unique constraint immediately
        }
        
        try {
            String response = processCommand(message);
            markEventProcessed(eventId);
            return response;
        } catch (Exception e) {
            log.error("Failed to process webhook. EventID: {}", eventId, e);
            markEventFailed(eventId);
            return "Sorry, I couldn't process that request right now.";
        }
    }
    
    // Separate transaction for safe status updates outside the main command transaction
    @Transactional
    public void markEventProcessed(String eventId) {
        eventRepository.findByProviderAndExternalEventId(ExternalIdentityProvider.WHATSAPP, eventId)
            .ifPresent(e -> {
                e.setStatus(ExternalWebhookEvent.EventStatus.PROCESSED);
                e.setProcessedAt(OffsetDateTime.now());
            });
    }

    @Transactional
    public void markEventFailed(String eventId) {
        eventRepository.findByProviderAndExternalEventId(ExternalIdentityProvider.WHATSAPP, eventId)
            .ifPresent(e -> e.setStatus(ExternalWebhookEvent.EventStatus.FAILED));
    }

    private String processCommand(WhatsAppMessage message) {
        String from = message.from();
        
        if (from == null || !from.startsWith("whatsapp:")) {
            log.warn("Malformed 'From' field: {}", from);
            return "Internal error: Invalid sender format";
        }
        
        String rawNumber = from.substring("whatsapp:".length());
        
        ExternalIdentity searchIdentity = new ExternalIdentity(ExternalIdentityProvider.WHATSAPP, rawNumber);
        Optional<User> resolvedUserOpt = identityResolver.resolve(searchIdentity);
        
        if (resolvedUserOpt.isEmpty()) {
            log.info("Unmapped or unverified WhatsApp identity.");
            return "Your WhatsApp number is not linked or verified.";
        }
        
        User user = resolvedUserOpt.get();
        log.info("Resolved identity to User ID: {}", user.getId());
        
        Optional<Command> commandOpt = commandParser.parse(user.getId(), message.body());
        
        if (commandOpt.isEmpty()) {
            log.info("Unsupported command received.");
            return "Supported command: add task: <task name>";
        }
        
        Command command = commandOpt.get();
        
        try {
            commandDispatcher.dispatch(command);
            log.info("Command {} successfully executed for User ID: {}", command.type(), user.getId());
            return "Task created successfully.";
        } catch (CommandException e) {
            log.warn("Command validation failed: {}", e.getMessage());
            return e.getMessage();
        }
    }
}
