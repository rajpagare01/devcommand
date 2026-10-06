package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.ai.config.AiProperties;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
public class FallbackAiIntentService implements NaturalLanguageInterpreter {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiIntentService.class);

    private final AiProperties properties;
    private final Map<String, AiIntentProvider> providers;

    public FallbackAiIntentService(AiProperties properties, List<AiIntentProvider> providerList) {
        this.properties = properties;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiIntentProvider::getProviderName, Function.identity()));
    }

    @Override
    public InterpretationResult interpret(Long userId, String naturalText, String conversationContext) {
        AiIntentProvider primary = providers.get(properties.getPrimaryProvider());
        if (primary == null) {
            log.error("Primary AI provider not found: {}", properties.getPrimaryProvider());
            return InterpretationResult.invalid();
        }

        try {
            ProviderIntentResult primaryResult = primary.interpret(naturalText, conversationContext);
            return mapToInterpretationResult(userId, primaryResult);
        } catch (TransientAiProviderException e) {
            log.warn("Primary AI provider ({}) transient failure: {}. Attempting fallback.", properties.getPrimaryProvider(), e.getMessage());
            return fallbackInterpret(userId, naturalText, conversationContext, e);
        } catch (CommandException e) {
            log.warn("Primary AI provider generated invalid parameters: {}", e.getMessage());
            throw e; // let parameter validation errors bubble up directly
        } catch (Exception e) {
            log.error("Primary AI provider ({}) permanent failure: {}", properties.getPrimaryProvider(), e.getMessage());
            // Do not fallback on non-transient failures
            throw new AiProviderException("Primary AI provider failure", e);
        }
    }

    private InterpretationResult fallbackInterpret(Long userId, String naturalText, String conversationContext, TransientAiProviderException primaryException) {
        String fallbackName = properties.getFallbackProvider();
        if (fallbackName == null || fallbackName.isBlank()) {
            log.warn("No fallback AI provider configured. Failing.");
            throw new AiProviderException("AI provider failure, no fallback configured", primaryException);
        }

        AiIntentProvider fallback = providers.get(fallbackName);
        if (fallback == null) {
            log.warn("Fallback AI provider not found: {}", fallbackName);
            throw new AiProviderException("AI provider failure, fallback provider missing", primaryException);
        }

        try {
            ProviderIntentResult fallbackResult = fallback.interpret(naturalText, conversationContext);
            log.info("Successfully used fallback AI provider: {}", fallbackName);
            return mapToInterpretationResult(userId, fallbackResult);
        } catch (CommandException e) {
            log.warn("Fallback AI provider generated invalid parameters: {}", e.getMessage());
            throw e; // let parameter validation errors bubble up directly
        } catch (Exception e) {
            log.error("Fallback AI provider ({}) also failed: {}", fallbackName, e.getMessage());
            // Fail completely
            throw new AiProviderException("Both primary and fallback AI providers failed", e);
        }
    }

    private InterpretationResult mapToInterpretationResult(Long userId, ProviderIntentResult providerResult) {
        switch (providerResult.status()) {
            case CLARIFICATION_REQUIRED:
                return InterpretationResult.clarification(providerResult.message(), providerResult.pendingContext());
            case UNSUPPORTED:
                return InterpretationResult.unsupported();
            case INVALID_MODEL_RESPONSE:
                return InterpretationResult.invalid();
            case READY:
                if (providerResult.intent() == null || providerResult.intent().action() == null) {
                    return InterpretationResult.invalid();
                }
                CommandType type;
                try {
                    type = CommandType.valueOf(providerResult.intent().action());
                } catch (IllegalArgumentException e) {
                    log.warn("AI provider returned unknown action: {}", providerResult.intent().action());
                    return InterpretationResult.invalid();
                }

                if (type.isReserved()) {
                    log.warn("AI provider returned reserved action: {}", type);
                    return InterpretationResult.unsupported();
                }

                Map<String, Object> paramsMap = providerResult.intent().parameters() != null 
                        ? providerResult.intent().parameters() 
                        : Collections.emptyMap();
                        
                CommandParameters params = new CommandParameters(paramsMap);
                Command command = new Command(type, userId, params);

                return InterpretationResult.ready(command);
            default:
                return InterpretationResult.invalid();
        }
    }
}
