package com.devcommand.devcommand.integrations.ai.tools;

import java.util.List;

public record ToolSchema(
        String description,
        List<ToolParameter> parameters
) {
    public static ToolSchema of(String description, ToolParameter... params) {
        return new ToolSchema(description, List.of(params));
    }
}
