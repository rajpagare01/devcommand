package com.devcommand.devcommand.command.parser;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicCommandParserLengthTest {

    private DeterministicCommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new DeterministicCommandParser();
    }

    @Test
    void parse_titleAtExactlyMaxLength_succeeds() {
        String title = "A".repeat(255);
        Optional<Command> result = parser.parse(1L, "add task: " + title);
        assertTrue(result.isPresent());
        assertEquals(CommandType.CREATE_TASK, result.get().type());
        assertEquals(title, result.get().parameters().optionalString("title").orElse(""));
    }

    @Test
    void parse_titleExceedsMaxLength_returnsEmpty() {
        String title = "A".repeat(256);
        Optional<Command> result = parser.parse(1L, "add task: " + title);
        assertFalse(result.isPresent());
    }

    @Test
    void parse_messageExceedsMaxMessageLength_returnsEmpty() {
        String oversized = "A".repeat(DeterministicCommandParser.MAX_MESSAGE_LENGTH + 1);
        Optional<Command> result = parser.parse(1L, oversized);
        assertFalse(result.isPresent());
    }

    @Test
    void parse_nullMessage_returnsEmpty() {
        Optional<Command> result = parser.parse(1L, null);
        assertFalse(result.isPresent());
    }
}
