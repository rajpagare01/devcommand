package com.devcommand.devcommand.integrations.ai.tools.jobs;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateJobApplicationTool implements AiTool {

    private final JobApplicationService jobApplicationService;

    @Override
    public String getName() {
        return "create_job_application";
    }

    @Override
    public String getDescription() {
        return "Records a new job application with company, role, status, location, and salary.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Creates a new job application",
                ToolParameter.required("company", "string", "Company name (e.g. Stripe, Google)"),
                ToolParameter.required("role", "string", "Role/title (e.g. Backend Engineer)"),
                ToolParameter.optional("status", "string", "Application status (SAVED, APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN)"),
                ToolParameter.optional("location", "string", "Job location or Remote"),
                ToolParameter.optional("salary", "string", "Salary compensation or range"),
                ToolParameter.optional("source", "string", "Source (e.g. LinkedIn, Referral)"),
                ToolParameter.optional("jobUrl", "string", "Posting URL"),
                ToolParameter.optional("notes", "string", "Notes or preparation thoughts")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String company = (String) parameters.get("company");
        String role = (String) parameters.get("role");

        if (company == null || company.isBlank()) {
            return ToolExecutionResult.failure("Company is required.");
        }
        if (role == null || role.isBlank()) {
            return ToolExecutionResult.failure("Role is required.");
        }

        String location = parameters.get("location") != null ? parameters.get("location").toString() : null;
        String jobUrl = parameters.get("jobUrl") != null ? parameters.get("jobUrl").toString() : null;
        String source = parameters.get("source") != null ? parameters.get("source").toString() : null;
        String salary = parameters.get("salary") != null ? parameters.get("salary").toString() : null;
        String notes = parameters.get("notes") != null ? parameters.get("notes").toString() : null;

        ApplicationStatus status = ApplicationStatus.APPLIED;
        if (parameters.get("status") != null) {
            try {
                status = ApplicationStatus.valueOf(parameters.get("status").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        CreateJobApplicationRequest request = new CreateJobApplicationRequest(
                company,
                role,
                location,
                jobUrl,
                source,
                salary,
                LocalDate.now(),
                status,
                notes
        );

        try {
            JobApplicationResponse response = jobApplicationService.create(request, userId);
            String summary = String.format("Recorded application for %s at %s (%s).",
                    response.role(), response.company(), response.status());
            return ToolExecutionResult.success(summary, response);
        } catch (Exception e) {
            log.error("Failed to create job application: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to create job application: " + e.getMessage());
        }
    }
}
