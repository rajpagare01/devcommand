package com.devcommand.devcommand.command;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandParametersTest {

    @Test
    void requiredString_ReturnsValue_WhenPresent() {
        CommandParameters params = new CommandParameters(Map.of("title", "Hello"));
        assertEquals("Hello", params.requiredString("title"));
    }

    @Test
    void requiredString_ThrowsException_WhenMissing() {
        CommandParameters params = new CommandParameters(Map.of());
        CommandException ex = assertThrows(CommandException.class, () -> params.requiredString("title"));
        assertEquals("Missing required parameter: title", ex.getMessage());
    }

    @Test
    void optionalString_ReturnsEmpty_WhenMissing() {
        CommandParameters params = new CommandParameters(Map.of());
        assertTrue(params.optionalString("desc").isEmpty());
    }

    enum TestEnum { ALPHA, BETA }

    @Test
    void optionalEnum_ReturnsValue_WhenPresentAndValid() {
        CommandParameters params = new CommandParameters(Map.of("type", "alpha"));
        assertEquals(TestEnum.ALPHA, params.optionalEnum(TestEnum.class, "type", TestEnum.BETA));
    }

    @Test
    void optionalEnum_ReturnsDefault_WhenMissing() {
        CommandParameters params = new CommandParameters(Map.of());
        assertEquals(TestEnum.BETA, params.optionalEnum(TestEnum.class, "type", TestEnum.BETA));
    }

    @Test
    void optionalEnum_ThrowsException_WhenInvalid() {
        CommandParameters params = new CommandParameters(Map.of("type", "INVALID"));
        CommandException ex = assertThrows(CommandException.class, () -> params.optionalEnum(TestEnum.class, "type", TestEnum.BETA));
        assertEquals("Invalid parameter: type", ex.getMessage());
    }

    @Test
    void optionalDate_ReturnsValue_WhenValid() {
        CommandParameters params = new CommandParameters(Map.of("date", "2026-10-01"));
        assertEquals(LocalDate.of(2026, 10, 1), params.optionalDate("date").get());
    }

    @Test
    void optionalDate_ThrowsException_WhenInvalid() {
        CommandParameters params = new CommandParameters(Map.of("date", "not-a-date"));
        assertThrows(CommandException.class, () -> params.optionalDate("date"));
    }
    
    @Test
    void rejectKey_ThrowsException_WhenKeyPresent() {
        CommandParameters params = new CommandParameters(Map.of("userId", "123"));
        assertThrows(CommandException.class, () -> params.rejectKey("userId"));
    }
}
