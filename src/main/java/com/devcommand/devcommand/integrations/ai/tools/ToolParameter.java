package com.devcommand.devcommand.integrations.ai.tools;

public record ToolParameter(
        String name,
        String type,
        String description,
        boolean required
) {
    public static ToolParameter required(String name, String type, String description) {
        return new ToolParameter(name, type, description, true);
    }

    public static ToolParameter optional(String name, String type, String description) {
        return new ToolParameter(name, type, description, false);
    }
}
