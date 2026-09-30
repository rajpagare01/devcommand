package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.integrations.whatsapp.config.TwilioProperties;
import com.twilio.security.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TwilioSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(TwilioSignatureValidator.class);
    private final TwilioProperties twilioProperties;
    private RequestValidator validator;

    public TwilioSignatureValidator(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
        if (twilioProperties.getAuthToken() != null && !twilioProperties.getAuthToken().isEmpty()) {
            this.validator = new RequestValidator(twilioProperties.getAuthToken());
        }
    }

    public boolean validate(String url, Map<String, String> params, String signature) {
        if (validator == null) {
            log.error("Twilio Auth Token is not configured. Failing closed.");
            return false;
        }
        
        if (signature == null || signature.isEmpty()) {
            log.warn("Missing Twilio signature.");
            return false;
        }

        boolean isValid = validator.validate(url, params, signature);
        if (!isValid) {
            log.warn("Twilio signature validation failed.");
        }
        
        return isValid;
    }
}
