package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.ai.config.AiProperties;
import com.devcommand.devcommand.integrations.gemini.service.IntentDto;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FallbackAiIntentServiceTest {

    private AiProperties properties;
    private AiIntentProvider geminiProvider;
    private AiIntentProvider groqProvider;
    private FallbackAiIntentService service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setPrimaryProvider("gemini");
        properties.setFallbackProvider("groq");

        geminiProvider = mock(AiIntentProvider.class);
        when(geminiProvider.getProviderName()).thenReturn("gemini");

        groqProvider = mock(AiIntentProvider.class);
        when(groqProvider.getProviderName()).thenReturn("groq");

        service = new FallbackAiIntentService(properties, List.of(geminiProvider, groqProvider));
    }

    @Test
    void interpret_usesPrimaryProvider_success() throws Exception {
        IntentDto intent = new IntentDto("CREATE_TASK", Map.of("title", "Test"), null, true);
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        InterpretationResult result = service.interpret(1L, "create task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertNotNull(result.command());
        assertEquals(CommandType.CREATE_TASK, result.command().type());
        assertEquals(1L, result.command().userId());

        verify(geminiProvider, times(1)).interpret(anyString(), any());
        verify(groqProvider, never()).interpret(anyString(), any());
    }

    @Test
    void interpret_fallsBackOnTransientFailure_success() throws Exception {
        when(geminiProvider.interpret(anyString(), any())).thenThrow(new TransientAiProviderException("timeout"));
        
        IntentDto intent = new IntentDto("CREATE_TASK", Map.of("title", "Test"), null, true);
        when(groqProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        InterpretationResult result = service.interpret(1L, "create task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertEquals(CommandType.CREATE_TASK, result.command().type());

        verify(geminiProvider, times(1)).interpret(anyString(), any());
        verify(groqProvider, times(1)).interpret(anyString(), any());
    }

    @Test
    void interpret_doesNotFallBackOnPermanentFailure() throws Exception {
        when(geminiProvider.interpret(anyString(), any())).thenThrow(new AiProviderException("bad request"));
        
        assertThrows(RuntimeException.class, () -> service.interpret(1L, "create task", null));

        verify(geminiProvider, times(1)).interpret(anyString(), any());
        verify(groqProvider, never()).interpret(anyString(), any());
    }

    @Test
    void interpret_returnsUnsupported_whenActionIsReserved() throws Exception {
        IntentDto intent = new IntentDto("READ_TASKS_TODAY", Map.of(), null, true);
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        InterpretationResult result = service.interpret(1L, "confirm this", null);

        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
        assertNull(result.command());
    }

    @Test
    void interpret_returnsInvalid_whenActionIsUnknown() throws Exception {
        IntentDto intent = new IntentDto("UNKNOWN_ACTION", Map.of(), null, true);
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        InterpretationResult result = service.interpret(1L, "do something", null);

        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        assertNull(result.command());
    }

    @Test
    void interpret_createDsaProblem_resolvesViaDomainEntityResolver() throws Exception {
        com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver entityResolver =
                mock(com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver.class);
        service = new FallbackAiIntentService(properties, List.of(geminiProvider), entityResolver);

        IntentDto intent = new IntentDto("CREATE_DSA_PROBLEM", Map.of("title", "Two Sum"), null, true);
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        when(entityResolver.resolveDsaProblem("Two Sum", null, null)).thenReturn(
                com.devcommand.devcommand.integrations.ai.resolver.DsaProblemResolution.resolved(
                        "Two Sum", "LEETCODE", com.devcommand.devcommand.dsa.entity.Difficulty.EASY, "Arrays & Hashing", "https://leetcode.com/problems/two-sum/"
                )
        );

        InterpretationResult result = service.interpret(1L, "Today I solved Two Sum on LeetCode", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertNotNull(result.command());
        assertEquals(CommandType.CREATE_DSA_PROBLEM, result.command().type());
        assertEquals("Two Sum", result.command().parameters().requiredString("title"));
        assertEquals("LEETCODE", result.command().parameters().requiredString("platform"));
        assertEquals("EASY", result.command().parameters().requiredString("difficulty"));
        assertEquals("Arrays & Hashing", result.command().parameters().requiredString("topic"));
        assertEquals("SOLVED", result.command().parameters().requiredString("status"));
    }

    @Test
    void interpret_createDsaProblem_ambiguous_returnsClarification() throws Exception {
        com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver entityResolver =
                mock(com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver.class);
        service = new FallbackAiIntentService(properties, List.of(geminiProvider), entityResolver);

        IntentDto intent = new IntentDto("CREATE_DSA_PROBLEM", Map.of("title", "Two Sum Special"), null, true);
        when(geminiProvider.interpret(anyString(), any())).thenReturn(ProviderIntentResult.ready(intent));

        when(entityResolver.resolveDsaProblem("Two Sum Special", null, null)).thenReturn(
                com.devcommand.devcommand.integrations.ai.resolver.DsaProblemResolution.ambiguous(
                        "Multiple matches found. Which one did you mean?"
                )
        );

        InterpretationResult result = service.interpret(1L, "I solved Two Sum Special", null);

        assertEquals(InterpretationStatus.CLARIFICATION_REQUIRED, result.status());
        assertEquals("Multiple matches found. Which one did you mean?", result.message());
        assertNull(result.command());
    }
}
