package com.devcommand.devcommand.integrations.ai.tools.jobs;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.dto.JobStatusUpdateRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateJobApplicationStatusTool implements AiTool {

    private final JobApplicationService jobApplicationService;
    private final JobApplicationRepository jobApplicationRepository;

    @Override
    public String getName() {
        return "update_job_application_status";
    }

    @Override
    public String getDescription() {
        return "Updates the status of a job application (e.g. SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN).";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Updates status of a job application",
                ToolParameter.optional("id", "integer", "ID of the job application"),
                ToolParameter.optional("company", "string", "Company name to find if ID is not provided"),
                ToolParameter.required("status", "string", "New status (SAVED, APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean requiresConfirmation(Map<String, Object> parameters, Long userId) {
        String statusStr = parameters.get("status") != null ? parameters.get("status").toString().toUpperCase() : "";
        // High-impact status changes require confirmation
        return "REJECTED".equalsIgnoreCase(statusStr) || "WITHDRAWN".equalsIgnoreCase(statusStr);
    }

    @Override
    public String getConfirmationMessage(Map<String, Object> parameters, Long userId) {
        String company = parameters.get("company") != null ? parameters.get("company").toString() : "ID #" + parameters.get("id");
        String status = parameters.get("status") != null ? parameters.get("status").toString().toUpperCase() : "UPDATED";
        return String.format("Are you sure you want to change the status of your application for %s to %s?", company, status);
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String statusStr = parameters.get("status") != null ? parameters.get("status").toString() : null;
        if (statusStr == null || statusStr.isBlank()) {
            return ToolExecutionResult.failure("Status is required.");
        }

        ApplicationStatus status;
        try {
            status = ApplicationStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolExecutionResult.failure("Invalid application status: " + statusStr);
        }

        Long id = null;
        if (parameters.get("id") != null) {
            try {
                id = Long.parseLong(parameters.get("id").toString());
            } catch (Exception ignored) {}
        }

        String company = parameters.get("company") != null ? parameters.get("company").toString() : null;
        Optional<JobApplication> jobOpt;
        if (id != null) {
            jobOpt = jobApplicationRepository.findByIdAndUserId(id, userId);
        } else if (company != null && !company.isBlank()) {
            jobOpt = jobApplicationRepository.findFirstByUserIdAndCompanyIgnoreCase(userId, company);
        } else {
            return ToolExecutionResult.failure("Either application ID or company name is required.");
        }

        if (jobOpt.isEmpty()) {
            return ToolExecutionResult.failure("Job application not found.");
        }

        JobApplication job = jobOpt.get();
        try {
            JobApplicationResponse response = jobApplicationService.changeStatus(
                    job.getId(), userId, new JobStatusUpdateRequest(status)
            );
            String summary = String.format("Updated application for %s at %s to %s.",
                    response.role(), response.company(), response.status());
            return ToolExecutionResult.success(summary, response);
        } catch (Exception e) {
            log.error("Failed to update job application status: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to update status: " + e.getMessage());
        }
    }
}
