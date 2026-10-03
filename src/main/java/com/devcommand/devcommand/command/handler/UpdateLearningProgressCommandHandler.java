package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateLearningProgressCommandHandler implements CommandHandler {

    private final LearningTopicService learningTopicService;
    private final LearningTopicRepository learningTopicRepository;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.UPDATE_LEARNING_PROGRESS;
    }

    @Override
    public CommandResult handle(Command command) {
        String topicName = command.parameters().requiredString("topic");
        Integer progress = command.parameters().optionalInteger("progress").orElse(null);
        Integer hours = command.parameters().optionalInteger("hours").orElse(null);

        // DB-scoped case-insensitive lookup — replaces in-memory findAll + equalsIgnoreCase scan.
        // Both the topic and technology match are evaluated in SQL, always scoped to command.userId().
        List<LearningTopic> matches = learningTopicRepository
                .findByUserIdAndTopicOrTechnologyIgnoreCase(command.userId(), topicName);

        if (matches.isEmpty()) {
            return CommandResult.failure("I couldn't find a learning topic matching '" + topicName + "'.");
        }

        // Use the first match — preserves original first-match semantics from the old loop.
        LearningTopic target = matches.get(0);

        if (progress != null && (progress < 0 || progress > 100)) {
            return CommandResult.failure("Progress must be between 0 and 100.");
        }

        if (hours != null && hours < 0) {
            return CommandResult.failure("Hours spent cannot be negative.");
        }

        if (hours != null) {
            Double existingHours = target.getHoursSpent() != null ? target.getHoursSpent() : 0.0;
            Double newHours = existingHours + hours;
            UpdateLearningTopicRequest updateRequest = new UpdateLearningTopicRequest(
                    target.getTechnology(),
                    target.getTopic(),
                    target.getProgress(),
                    target.getStatus(),
                    newHours,
                    target.getResourceUrl(),
                    target.getNotes()
            );
            var response = learningTopicService.update(target.getId(), command.userId(), updateRequest);
            return CommandResult.success("Added " + hours + " hours. Total spent on '" + target.getTopic() + "' is now " + newHours + " hours.", response);
        } else if (progress != null) {
            var response = learningTopicService.updateProgress(target.getId(), command.userId(), new ProgressUpdateRequest(progress));
            return CommandResult.success("Updated progress for '" + target.getTopic() + "' to " + progress + "%.", response);
        } else {
            return CommandResult.success("Updated learning activity for '" + target.getTopic() + "'.", target);
        }
    }
}
