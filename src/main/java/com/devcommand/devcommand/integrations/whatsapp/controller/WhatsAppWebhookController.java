package com.devcommand.devcommand.integrations.whatsapp.controller;

import com.devcommand.devcommand.integrations.whatsapp.dto.WhatsAppMessage;
import com.devcommand.devcommand.integrations.whatsapp.service.TwilioSignatureValidator;
import com.devcommand.devcommand.integrations.whatsapp.service.WhatsAppWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);
    private final TwilioSignatureValidator signatureValidator;
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(TwilioSignatureValidator signatureValidator, 
                                     WhatsAppWebhookService webhookService) {
        this.signatureValidator = signatureValidator;
        this.webhookService = webhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleWebhook(HttpServletRequest request, @RequestParam Map<String, String> allParams) {
        String signature = request.getHeader("X-Twilio-Signature");
        
        // Reconstruct URL. In production behind a proxy, ensure ForwardedHeaderFilter is enabled.
        StringBuffer urlBuffer = request.getRequestURL();
        if (request.getQueryString() != null) {
            urlBuffer.append("?").append(request.getQueryString());
        }
        String url = urlBuffer.toString();

        if (!signatureValidator.validate(url, allParams, signature)) {
            log.warn("Invalid Twilio signature from {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WhatsAppMessage message = WhatsAppMessage.fromForm(allParams);
        String responseText = webhookService.handleMessage(message);

        String xmlResponse = buildTwiML(responseText);
        return ResponseEntity.ok(xmlResponse);
    }
    
    private String buildTwiML(String text) {
        if (text == null || text.isEmpty()) {
            return "<Response></Response>";
        }
        return "<Response><Message>" + escapeXml(text) + "</Message></Response>";
    }
    
    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
