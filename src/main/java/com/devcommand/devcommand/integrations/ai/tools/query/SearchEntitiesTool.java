package com.devcommand.devcommand.integrations.ai.tools.query;

import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.repository.DsaProblemSpecifications;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.service.ProjectService;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.mapper.DailyTaskMapper;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.tasks.repository.DailyTaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchEntitiesTool implements AiTool {

    private final DsaProblemRepository dsaProblemRepository;
    private final DsaProblemMapper dsaProblemMapper;
    private final JobApplicationService jobApplicationService;
    private final ProjectService projectService;
    private final DailyTaskRepository dailyTaskRepository;
    private final DailyTaskMapper dailyTaskMapper;

    @Override
    public String getName() {
        return "search_entities";
    }

    @Override
    public String getDescription() {
        return "Cross-domain search across DSA problems, job applications, projects, and daily tasks.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Searches for matching entities across all DevCommand modules",
                ToolParameter.required("query", "string", "Keywords or title to search for across modules")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            return ToolExecutionResult.failure("Search query is required.");
        }

        String lowerQuery = "%" + query.trim().toLowerCase() + "%";

        // 1. Search DSA problems
        Specification<DsaProblem> dsaSpec = Specification.allOf(
                DsaProblemSpecifications.ownerIs(userId),
                (root, q, cb) -> cb.or(
                        cb.like(cb.lower(root.get("title")), lowerQuery),
                        cb.like(cb.lower(root.get("topic")), lowerQuery)
                )
        );
        List<?> dsaResults = dsaProblemRepository.findAll(dsaSpec, PageRequest.of(0, 5))
                .map(dsaProblemMapper::toResponse)
                .getContent();

        // 2. Search Job applications
        List<JobApplicationResponse> jobResults = jobApplicationService.getAll(
                userId, null, null, null, null, query.trim(), 0, 5, "createdAt", "DESC"
        ).getContent();

        // 3. Search Projects
        List<ProjectResponse> projectResults = projectService.getAll(
                userId, null, query.trim(), 0, 5, "createdAt", "DESC"
        ).getContent();

        // 4. Search Daily tasks
        Specification<DailyTask> taskSpec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                (root, q, cb) -> cb.or(
                        cb.like(cb.lower(root.get("title")), lowerQuery),
                        cb.like(cb.lower(root.get("description")), lowerQuery)
                )
        );
        List<DailyTaskResponse> taskResults = dailyTaskRepository.findAll(taskSpec, PageRequest.of(0, 5))
                .map(dailyTaskMapper::toResponse)
                .getContent();

        Map<String, Object> allResults = new LinkedHashMap<>();
        allResults.put("dsaProblems", dsaResults);
        allResults.put("jobApplications", jobResults);
        allResults.put("projects", projectResults);
        allResults.put("tasks", taskResults);

        int totalFound = dsaResults.size() + jobResults.size() + projectResults.size() + taskResults.size();
        String summary = String.format("Found %d matches for '%s' across domains (DSA: %d, Jobs: %d, Projects: %d, Tasks: %d).",
                totalFound, query, dsaResults.size(), jobResults.size(), projectResults.size(), taskResults.size());

        return ToolExecutionResult.success(summary, allResults);
    }
}
