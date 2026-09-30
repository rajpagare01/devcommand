package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.service.VerificationChallengeSender;
import com.devcommand.devcommand.integrations.whatsapp.config.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TwilioVerificationChallengeSender implements VerificationChallengeSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioVerificationChallengeSender.class);
    private final TwilioProperties twilioProperties;

    public TwilioVerificationChallengeSender(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    @PostConstruct
    public void init() {
        if (twilioProperties.getAccountSid() != null && twilioProperties.getAuthToken() != null) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
        } else {
            log.warn("Twilio credentials not fully configured. Twilio verification delivery will fail.");
        }
    }

    @Override
    public boolean supports(ExternalIdentityProvider provider) {
        return provider == ExternalIdentityProvider.WHATSAPP;
    }

    @Override
    public void sendVerificationChallenge(String externalId, String challengeCode) {
        if (twilioProperties.getAccountSid() == null || twilioProperties.getAuthToken() == null) {
            log.error("Cannot send verification challenge. Twilio credentials missing.");
            throw new RuntimeException("Twilio credentials not configured");
        }

        String to = externalId;
        if (!to.startsWith("whatsapp:")) {
            to = "whatsapp:" + to;
        }

        String from = twilioProperties.getWhatsappNumber();
        if (from != null && !from.startsWith("whatsapp:")) {
            from = "whatsapp:" + from;
        }

        try {
            var creator = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    "Your DevCommand verification code is: " + challengeCode
            );
            
            if (twilioProperties.getContentSid() != null) {
                creator.setContentSid(twilioProperties.getContentSid());
                // Use generic variables in case the sandbox uses the verification or appointment template
                creator.setContentVariables("{\"1\":\"DevCommand\",\"2\":\"" + challengeCode + "\"}");
            }

            Message message = creator.create();
            
            log.info("Successfully handed off verification challenge to Twilio. Message SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Failed to send verification challenge via Twilio", e);
            throw new RuntimeException("Failed to send verification message", e);
        }
    }
}
