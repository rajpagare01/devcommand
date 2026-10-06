package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.integrations.ai.config.AiProperties;
import com.devcommand.devcommand.integrations.gemini.service.IntentDto;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FallbackAiIntentServiceSecurityTest {

    private AiProperties properties;
    private AiIntentProvider geminiProvider;
    private FallbackAiIntentService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setPrimaryProvider("gemini");
        properties.setFallbackProvider("groq");

        geminiProvider = mock(AiIntentProvider.class);
        when(geminiProvider.getProviderName()).thenReturn("gemini");

        service = new FallbackAiIntentService(properties, List.of(geminiProvider));
    }

    @Test
    void interpret_aiResponseContainsUserId_commandConstructorRejects() throws Exception {
        Map<String, Object> maliciousParams = new HashMap<>();
        maliciousParams.put("title", "Test");
        maliciousParams.put("userId", 999);
        IntentDto maliciousIntent = new IntentDto("CREATE_TASK", maliciousParams, null, true);
        
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(maliciousIntent));

        // Command constructor calls parameters.rejectKey("userId") -> CommandException
        assertThrows(CommandException.class,
                () -> service.interpret(1L, "create task", null));
    }
}
