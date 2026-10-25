package com.devcommand.devcommand.integrations.ai.tools.jobs;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListJobApplicationsTool implements AiTool {

    private final JobApplicationService jobApplicationService;

    @Override
    public String getName() {
        return "list_job_applications";
    }

    @Override
    public String getDescription() {
        return "Lists job applications with optional filters by company, role, status, or search query.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Lists job applications",
                ToolParameter.optional("company", "string", "Filter by company name"),
                ToolParameter.optional("role", "string", "Filter by role/title"),
                ToolParameter.optional("status", "string", "Filter by status (APPLIED, INTERVIEW, OFFER, etc.)"),
                ToolParameter.optional("search", "string", "Search query"),
                ToolParameter.optional("size", "integer", "Max results (default 10)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String company = parameters.get("company") != null ? parameters.get("company").toString() : null;
        String role = parameters.get("role") != null ? parameters.get("role").toString() : null;
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        String search = parameters.get("search") != null ? parameters.get("search").toString() : null;

        int size = 10;
        if (parameters.get("size") != null) {
            try {
                size = Integer.parseInt(parameters.get("size").toString());
                if (size <= 0) size = 10;
            } catch (Exception ignored) {}
        }

        Page<JobApplicationResponse> page = jobApplicationService.getAll(
                userId, company, role, null, status, search, 0, size, "createdAt", "DESC"
        );

        String summary = String.format("Found %d job applications (showing %d).",
                page.getTotalElements(), page.getNumberOfElements());

        return ToolExecutionResult.success(summary, page.getContent());
    }
}
