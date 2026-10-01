package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReadJobPipelineCommandHandler implements CommandHandler {

    private final JobApplicationRepository jobApplicationRepository;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.READ_JOB_PIPELINE;
    }

    @Override
    public CommandResult handle(Command command) {
        JobApplicationRepository.JobsOverviewProjection overview = jobApplicationRepository.getJobsOverviewByUserId(command.userId());
        List<JobApplicationRepository.ApplicationStatusCount> counts = jobApplicationRepository.countStatusByUserId(command.userId());

        if (overview == null || overview.getTotal() == null || overview.getTotal() == 0) {
            return CommandResult.success("You haven't tracked any job applications yet.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🏢 *Job Application Pipeline*\n\n");
        sb.append("Total tracked: ").append(overview.getTotal()).append("\n");
        sb.append("Active pipeline: ").append(overview.getActive() != null ? overview.getActive() : 0).append("\n\n");
        
        sb.append("*Status Counts:*\n");
        for (JobApplicationRepository.ApplicationStatusCount count : counts) {
            sb.append("- ").append(count.getStatus()).append(": ").append(count.getCount()).append("\n");
        }

        return CommandResult.success(sb.toString().trim());
    }
}
