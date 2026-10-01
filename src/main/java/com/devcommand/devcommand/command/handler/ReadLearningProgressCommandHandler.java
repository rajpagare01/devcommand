package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.learning.dto.LearningSummary;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReadLearningProgressCommandHandler implements CommandHandler {

    private final LearningTopicService learningTopicService;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.READ_LEARNING_PROGRESS;
    }

    @Override
    public CommandResult handle(Command command) {
        LearningSummary summary = learningTopicService.getSummary(command.userId());
        
        if (summary.totalTopics() == 0) {
            return CommandResult.success("You haven't tracked any learning topics yet. Time to start learning something new!");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📚 *Learning Progress*\n\n");
        sb.append("Total topics: ").append(summary.totalTopics()).append("\n");
        sb.append("In progress: ").append(summary.inProgressTopics()).append("\n");
        sb.append("Completed: ").append(summary.completedTopics()).append("\n");
        sb.append(String.format("Average progress: %.1f%%\n", summary.averageProgress()));
        
        List<LearningTopicResponse> inProgress = learningTopicService.inProgressTopics(command.userId());
        if (!inProgress.isEmpty()) {
            sb.append("\n*Currently Learning:*\n");
            for (int i = 0; i < Math.min(inProgress.size(), 5); i++) {
                LearningTopicResponse t = inProgress.get(i);
                sb.append("- ").append(t.topic()).append(" (").append(t.progress()).append("%)\n");
            }
        }
        
        return CommandResult.success(sb.toString().trim());
    }
}
