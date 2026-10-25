package com.devcommand.devcommand.integrations.ai.tools;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiToolRegistry {

    private final Map<String, AiTool> tools;

    public AiToolRegistry(List<AiTool> toolList) {
        this.tools = toolList.stream()
                .collect(Collectors.toMap(
                        t -> t.getName().toLowerCase(),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public Optional<AiTool> getTool(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name.trim().toLowerCase()));
    }

    public Collection<AiTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public boolean hasTool(String name) {
        return name != null && tools.containsKey(name.trim().toLowerCase());
    }
}
