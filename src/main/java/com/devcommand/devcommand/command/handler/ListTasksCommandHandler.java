package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListTasksCommandHandler implements CommandHandler {

    private final DailyTaskService dailyTaskService;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.READ_PENDING_TASKS;
    }

    @Override
    public CommandResult handle(Command command) {
        Page<DailyTaskResponse> tasksPage = dailyTaskService.getPending(
                command.userId(),
                PageRequest.of(0, 11, Sort.by(Sort.Direction.ASC, "dueDate"))
        );

        List<DailyTaskResponse> tasks = tasksPage.getContent();

        if (tasks.isEmpty()) {
            return CommandResult.success("You have no pending tasks! \uD83C\uDF89"); // 🎉
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Pending Tasks*\n\n");

        int limit = Math.min(tasks.size(), 10);
        for (int i = 0; i < limit; i++) {
            DailyTaskResponse task = tasks.get(i);
            String title = task.title()
                    .replace("*", "\\*")
                    .replace("_", "\\_")
                    .replace("[", "\\[")
                    .replace("`", "\\`");
            
            sb.append(String.format("ID: %d - %s [%s]\n", task.id(), title, task.priority()));
        }

        if (tasks.size() > 10) {
            sb.append("\n_...and more pending tasks not shown._");
        }

        return CommandResult.success(sb.toString());
    }
}
