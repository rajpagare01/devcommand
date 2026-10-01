package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicCommandParserTest {

    private final DeterministicCommandParser parser = new DeterministicCommandParser();

    @Test
    void parse_listTasks_exact() {
        Optional<Command> cmd = parser.parse(1L, "list tasks");
        assertTrue(cmd.isPresent());
        assertEquals(CommandType.READ_PENDING_TASKS, cmd.get().type());
        assertEquals(1L, cmd.get().userId());
    }

    @Test
    void parse_listTasks_caseInsensitive() {
        Optional<Command> cmd = parser.parse(1L, "LiSt TAsKs");
        assertTrue(cmd.isPresent());
        assertEquals(CommandType.READ_PENDING_TASKS, cmd.get().type());
    }

    @Test
    void parse_listTasks_leadingTrailingWhitespace() {
        Optional<Command> cmd = parser.parse(1L, "  list tasks  \n");
        assertTrue(cmd.isPresent());
        assertEquals(CommandType.READ_PENDING_TASKS, cmd.get().type());
    }

    @Test
    void parse_listTasks_malformed_returnsEmpty() {
        Optional<Command> cmd = parser.parse(1L, "list tasks please");
        assertTrue(cmd.isEmpty());

        cmd = parser.parse(1L, "list my tasks");
        assertTrue(cmd.isEmpty());
    }

    @Test
    void parse_createTask_stillWorks() {
        Optional<Command> cmd = parser.parse(1L, "add task: test");
        assertTrue(cmd.isPresent());
        assertEquals(CommandType.CREATE_TASK, cmd.get().type());
        assertEquals("test", cmd.get().parameters().requiredString("title"));
    }
}
