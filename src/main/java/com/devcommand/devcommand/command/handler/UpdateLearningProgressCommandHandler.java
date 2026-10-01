package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.learning.repository.LearningTopicSpecifications;
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

        // Find the topic to update
        List<LearningTopic> topics = learningTopicRepository.findAll(LearningTopicSpecifications.ownerIs(command.userId()));
        
        LearningTopic target = null;
        for (LearningTopic t : topics) {
            if (t.getTopic().equalsIgnoreCase(topicName) || t.getTechnology().equalsIgnoreCase(topicName)) {
                target = t;
                break;
            }
        }
        
        if (target == null) {
            return CommandResult.failure("I couldn't find a learning topic matching '" + topicName + "'.");
        }
        
        if (progress != null && (progress < 0 || progress > 100)) {
            return CommandResult.failure("Progress must be between 0 and 100.");
        }
        
        if (hours != null && hours < 0) {
            return CommandResult.failure("Hours spent cannot be negative.");
        }
        
        if (hours != null) {
            Double existingHours = target.getHoursSpent() != null ? target.getHoursSpent() : 0.0;
            Double newHours = existingHours + hours;
            com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest updateRequest = 
                new com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest(
                    target.getTechnology(),
                    target.getTopic(),
                    target.getProgress(),
                    target.getStatus(),
                    newHours,
                    target.getResourceUrl(),
                    target.getNotes()
                );
            learningTopicService.update(target.getId(), command.userId(), updateRequest);
            return CommandResult.success("Added " + hours + " hours. Total spent on '" + target.getTopic() + "' is now " + newHours + " hours.");
        } else if (progress != null) {
            learningTopicService.updateProgress(target.getId(), command.userId(), new ProgressUpdateRequest(progress));
            return CommandResult.success("Updated progress for '" + target.getTopic() + "' to " + progress + "%.");
        } else {
            return CommandResult.success("Updated learning activity for '" + target.getTopic() + "'.");
        }
    }
}
