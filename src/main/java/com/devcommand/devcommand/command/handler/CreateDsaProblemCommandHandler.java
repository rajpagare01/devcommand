package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CreateDsaProblemCommandHandler implements CommandHandler {

    private final DsaProblemService dsaProblemService;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CREATE_DSA_PROBLEM;
    }

    @Override
    public CommandResult handle(Command command) {
        String platform = command.parameters().requiredString("platform");
        String title = command.parameters().requiredString("title");
        Difficulty difficulty = command.parameters().optionalEnum(Difficulty.class, "difficulty", Difficulty.EASY);
        String topic = command.parameters().optionalString("topic").orElse("Uncategorized");
        ProblemStatus status = command.parameters().optionalEnum(ProblemStatus.class, "status", ProblemStatus.SOLVED);
        Integer timeTaken = command.parameters().optionalInteger("timeTaken").orElse(null);

        if (timeTaken != null && timeTaken < 0) {
            return CommandResult.failure("Time taken cannot be negative.");
        }

        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                title,
                platform,
                null, // problemUrl
                topic,
                difficulty,
                status,
                LocalDate.now(), // dateSolved
                timeTaken,
                null, // notes
                null // revisionDate
        );

        dsaProblemService.create(request, command.userId());
        return CommandResult.success("Tracked " + difficulty + " problem '" + title + "' on " + platform + "!");
    }
}
