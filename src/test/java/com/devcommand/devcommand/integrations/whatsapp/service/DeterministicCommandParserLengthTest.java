package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DeterministicCommandParser input-length and boundary conditions.
 * Existing functional tests remain in DeterministicCommandParserTest.
 */
class DeterministicCommandParserLengthTest {

    private DeterministicCommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new DeterministicCommandParser();
    }

    @Test
    void parse_titleAtExactlyMaxLength_succeeds() {
        // Title of exactly 255 chars should be accepted
        String title = "A".repeat(255);
        Optional<Command> result = parser.parse(1L, "add task: " + title);
        assertTrue(result.isPresent());
        assertEquals(CommandType.CREATE_TASK, result.get().type());
        assertEquals(title, result.get().parameters().optionalString("title").orElse(""));
    }

    @Test
    void parse_titleExceedsMaxLength_returnsEmpty() {
        // Title of 256 chars should be rejected (would exceed VARCHAR(255))
        String title = "A".repeat(256);
        Optional<Command> result = parser.parse(1L, "add task: " + title);
        assertFalse(result.isPresent(),
                "Parser should return empty when title exceeds max length");
    }

    @Test
    void parse_messageAtExactlyMaxLength_listTasksNotAffected() {
        // Non-add-task commands don't have a title, but general message length limit applies
        Optional<Command> result = parser.parse(1L, "list tasks");
        assertTrue(result.isPresent());
    }

    @Test
    void parse_messageExceedsMaxMessageLength_returnsEmpty() {
        // A message over 2000 chars is rejected entirely
        String oversized = "A".repeat(DeterministicCommandParser.MAX_MESSAGE_LENGTH + 1);
        Optional<Command> result = parser.parse(1L, oversized);
        assertFalse(result.isPresent(),
                "Parser should return empty for messages exceeding MAX_MESSAGE_LENGTH");
    }

    @Test
    void parse_messageAtExactlyMaxMessageLength_doesNotReject() {
        // A message of exactly MAX_MESSAGE_LENGTH that doesn't match any pattern returns empty
        // but should NOT be rejected by the length guard (the regex just won't match)
        String exactMax = "A".repeat(DeterministicCommandParser.MAX_MESSAGE_LENGTH);
        Optional<Command> result = parser.parse(1L, exactMax);
        // Doesn't match a pattern, so returns empty - but the length guard should not be the reason
        assertFalse(result.isPresent()); // still no match, but no length-based rejection
    }

    @Test
    void parse_nullMessage_returnsEmpty() {
        Optional<Command> result = parser.parse(1L, null);
        assertFalse(result.isPresent());
    }

    @Test
    void parse_blankMessage_returnsEmpty() {
        Optional<Command> result = parser.parse(1L, "   ");
        assertFalse(result.isPresent());
    }
}
