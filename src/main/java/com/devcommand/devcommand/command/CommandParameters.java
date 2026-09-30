package com.devcommand.devcommand.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class CommandParameters {
    private final Map<String, Object> params;

    @SuppressWarnings("unchecked")
    public CommandParameters(Map<String, ?> params) {
        this.params = params == null ? Collections.emptyMap() : Collections.unmodifiableMap((Map<String, Object>) params);
    }

    public String requiredString(String key) {
        String val = optionalString(key).orElse("");
        if (val.isBlank()) {
            throw new CommandException("Missing required parameter: " + key);
        }
        return val;
    }

    public Optional<String> optionalString(String key) {
        Object val = params.get(key);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(val).trim());
    }

    public <E extends Enum<E>> E optionalEnum(Class<E> enumClass, String key, E defaultValue) {
        Optional<String> val = optionalString(key);
        if (val.isEmpty() || val.get().isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, val.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandException("Invalid parameter: " + key);
        }
    }

    public Optional<LocalDate> optionalDate(String key) {
        Optional<String> val = optionalString(key);
        if (val.isEmpty() || val.get().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(val.get()));
        } catch (DateTimeParseException e) {
            throw new CommandException("Invalid parameter: " + key);
        }
    }
    
    // Safety check to ensure handlers don't accidentally extract a userId override
    public void rejectKey(String key) {
        if (params.containsKey(key)) {
            throw new CommandException("Unauthorized parameter: " + key);
        }
    }
    
    public Map<String, Object> asMap() {
        return params;
    }
}
