package com.devcommand.devcommand.integrations.whatsapp.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicCommandParser {

    private static final Pattern CREATE_TASK_PATTERN = Pattern.compile("(?i)^add\\s+task:\\s*(.+)$");

    public Optional<Command> parse(Long userId, String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return Optional.empty();
        }

        String body = messageBody.trim();
        
        Matcher taskMatcher = CREATE_TASK_PATTERN.matcher(body);
        if (taskMatcher.matches()) {
            String title = taskMatcher.group(1).trim();
            Map<String, Object> params = new HashMap<>();
            params.put("title", title);
            
            Command command = new Command(
                    CommandType.CREATE_TASK,
                    userId, 
                    new CommandParameters(params)
            );
            return Optional.of(command);
        }

        if (body.equalsIgnoreCase("list tasks")) {
            return Optional.of(new Command(
                    CommandType.READ_PENDING_TASKS,
                    userId,
                    new CommandParameters(new HashMap<>())
            ));
        }

        return Optional.empty();
    }
}
