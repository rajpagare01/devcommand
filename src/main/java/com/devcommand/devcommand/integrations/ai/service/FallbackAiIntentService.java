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
    private final com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver domainEntityResolver;

    public FallbackAiIntentService(AiProperties properties, List<AiIntentProvider> providerList) {
        this(properties, providerList, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FallbackAiIntentService(
            AiProperties properties,
            List<AiIntentProvider> providerList,
            com.devcommand.devcommand.integrations.ai.resolver.DomainEntityResolver domainEntityResolver) {
        this.properties = properties;
        this.domainEntityResolver = domainEntityResolver;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiIntentProvider::getProviderName, Function.identity()));
    }

    @Override
    public InterpretationResult interpret(Long userId, String naturalText, String conversationContext) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated userId is required for interpretation");
        }
        AiIntentProvider primary = providers.get(properties.getPrimaryProvider());
        if (primary == null) {
            log.error("Primary AI provider not found: {}", properties.getPrimaryProvider());
            return InterpretationResult.invalid();
        }

        try {
            ProviderIntentResult primaryResult = primary.interpret(naturalText, conversationContext);
            return mapToInterpretationResult(userId, primaryResult, conversationContext);
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
            return mapToInterpretationResult(userId, fallbackResult, conversationContext);
        } catch (CommandException e) {
            log.warn("Fallback AI provider generated invalid parameters: {}", e.getMessage());
            throw e; // let parameter validation errors bubble up directly
        } catch (Exception e) {
            log.error("Fallback AI provider ({}) also failed: {}", fallbackName, e.getMessage());
            // Fail completely
            throw new AiProviderException("Both primary and fallback AI providers failed", e);
        }
    }

    private InterpretationResult mapToInterpretationResult(Long userId, ProviderIntentResult providerResult, String conversationContext) {
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

                // Security invariant: LLM must NEVER supply client-controlled userId or ownership identifiers
                if (paramsMap.containsKey("userId") || paramsMap.containsKey("user_id") || paramsMap.containsKey("ownerId")) {
                    log.warn("Security violation: LLM generated user ownership identifier in parameters");
                    throw new CommandException("Client-supplied user ID is strictly rejected for security.");
                }

                Map<String, Object> resolvedMap = new java.util.HashMap<>(paramsMap);

                // Domain entity resolution
                if (domainEntityResolver != null) {
                    if (type == CommandType.CREATE_DSA_PROBLEM) {
                        String rawTitle = paramsMap.get("title") != null ? paramsMap.get("title").toString() : null;
                        String rawPlatform = paramsMap.get("platform") != null ? paramsMap.get("platform").toString() : null;
                        com.devcommand.devcommand.dsa.entity.Difficulty explicitDiff = null;
                        if (paramsMap.get("difficulty") != null) {
                            try {
                                explicitDiff = com.devcommand.devcommand.dsa.entity.Difficulty.valueOf(paramsMap.get("difficulty").toString().toUpperCase());
                            } catch (Exception ignored) {}
                        }

                        com.devcommand.devcommand.integrations.ai.resolver.DsaProblemResolution dsaRes =
                                domainEntityResolver.resolveDsaProblem(rawTitle, rawPlatform, explicitDiff);

                        if (!dsaRes.isResolved()) {
                            return InterpretationResult.clarification(dsaRes.clarificationMessage(), Map.of("action", "CREATE_DSA_PROBLEM", "parameters", paramsMap));
                        }

                        resolvedMap.put("title", dsaRes.title());
                        resolvedMap.put("platform", dsaRes.platform());
                        resolvedMap.put("difficulty", dsaRes.difficulty().name());
                        resolvedMap.put("topic", dsaRes.topic() != null ? dsaRes.topic() : "Uncategorized");
                        resolvedMap.put("status", paramsMap.getOrDefault("status", "SOLVED"));
                    } else if (type == CommandType.COMPLETE_TASK || type == CommandType.DELETE_TASK) {
                        boolean hasExplicitId = paramsMap.get("id") != null || paramsMap.get("taskId") != null;
                        if (!hasExplicitId && paramsMap.get("title") != null) {
                            String rawTitle = paramsMap.get("title").toString();
                            com.devcommand.devcommand.integrations.ai.resolver.TaskResolution taskRes =
                                    domainEntityResolver.resolveTask(userId, rawTitle, conversationContext);

                            if (!taskRes.isResolved()) {
                                return InterpretationResult.clarification(taskRes.clarificationMessage(), Map.of("action", type.name(), "parameters", paramsMap));
                            }

                            resolvedMap.put("title", taskRes.title());
                            if (type == CommandType.COMPLETE_TASK) {
                                resolvedMap.put("id", String.valueOf(taskRes.taskId()));
                            } else {
                                resolvedMap.put("taskId", String.valueOf(taskRes.taskId()));
                            }
                        }
                    } else if (type == CommandType.CREATE_TASK) {
                        String rawTitle = paramsMap.get("title") != null ? paramsMap.get("title").toString() : null;
                        if ("it".equalsIgnoreCase(rawTitle) || "that".equalsIgnoreCase(rawTitle)) {
                            com.devcommand.devcommand.integrations.ai.resolver.TaskResolution taskRes =
                                    domainEntityResolver.resolveTask(userId, rawTitle, conversationContext);
                            if (taskRes.isResolved()) {
                                resolvedMap.put("title", taskRes.title());
                            } else {
                                return InterpretationResult.clarification("I'm not sure which task you mean. Please specify the task title.", Map.of("action", "CREATE_TASK", "parameters", paramsMap));
                            }
                        }
                    }
                }
                        
                CommandParameters params = new CommandParameters(resolvedMap);
                Command command = new Command(type, userId, params);

                return InterpretationResult.ready(command);
            default:
                return InterpretationResult.invalid();
        }
    }
}
