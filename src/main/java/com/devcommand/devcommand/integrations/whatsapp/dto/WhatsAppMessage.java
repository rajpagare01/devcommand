package com.devcommand.devcommand.integrations.whatsapp.dto;

import java.util.Map;

public record WhatsAppMessage(
        String from,
        String to,
        String body,
        String messageSid
) {
    public static WhatsAppMessage fromForm(Map<String, String> form) {
        return new WhatsAppMessage(
                form.get("From"),
                form.get("To"),
                form.get("Body"),
                form.get("MessageSid")
        );
    }
}
