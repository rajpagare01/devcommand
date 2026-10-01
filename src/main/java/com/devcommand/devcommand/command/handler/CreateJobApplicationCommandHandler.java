package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CreateJobApplicationCommandHandler implements CommandHandler {

    private final JobApplicationService jobApplicationService;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CREATE_JOB_APPLICATION;
    }

    @Override
    public CommandResult handle(Command command) {
        String company = command.parameters().requiredString("company");
        String role = command.parameters().requiredString("role");
        ApplicationStatus status = command.parameters().optionalEnum(ApplicationStatus.class, "status", ApplicationStatus.APPLIED);
        String location = command.parameters().optionalString("location").orElse(null);
        String salary = command.parameters().optionalString("salary").orElse(null);
        String source = command.parameters().optionalString("source").orElse(null);

        CreateJobApplicationRequest request = new CreateJobApplicationRequest(
                company,
                role,
                location,
                null, // jobUrl
                source,
                salary,
                LocalDate.now(), // applicationDate
                status,
                null // notes
        );

        jobApplicationService.create(request, command.userId());
        return CommandResult.success("Tracked application for " + role + " at " + company + " (" + status + ")!");
    }
}
