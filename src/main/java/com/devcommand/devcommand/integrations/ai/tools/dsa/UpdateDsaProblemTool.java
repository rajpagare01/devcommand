package com.devcommand.devcommand.integrations.ai.tools.dsa;

import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.dto.UpdateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateDsaProblemTool implements AiTool {

    private final DsaProblemService dsaProblemService;
    private final DsaProblemRepository dsaProblemRepository;

    @Override
    public String getName() {
        return "update_dsa_problem";
    }

    @Override
    public String getDescription() {
        return "Updates an existing DSA problem's status, notes, difficulty, or time taken.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Updates an existing DSA problem",
                ToolParameter.optional("id", "integer", "ID of the DSA problem"),
                ToolParameter.optional("title", "string", "Title of the problem to find if ID is not known"),
                ToolParameter.optional("status", "string", "New status (SOLVED, REVISION, MASTERED, TODO)"),
                ToolParameter.optional("difficulty", "string", "Updated difficulty"),
                ToolParameter.optional("topic", "string", "Updated topic"),
                ToolParameter.optional("notes", "string", "Updated notes"),
                ToolParameter.optional("timeTaken", "integer", "Time taken in minutes")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        Long problemId = null;
        if (parameters.get("id") != null) {
            try {
                problemId = Long.parseLong(parameters.get("id").toString());
            } catch (Exception ignored) {}
        }

        String title = parameters.get("title") != null ? parameters.get("title").toString() : null;

        Optional<DsaProblem> existingOpt;
        if (problemId != null) {
            existingOpt = dsaProblemRepository.findByIdAndUserId(problemId, userId);
        } else if (title != null && !title.isBlank()) {
            existingOpt = dsaProblemRepository.findFirstByUserIdAndTitleIgnoreCase(userId, title);
        } else {
            return ToolExecutionResult.failure("Either problem ID or title is required to update a DSA problem.");
        }

        if (existingOpt.isEmpty()) {
            return ToolExecutionResult.failure("DSA problem not found.");
        }

        DsaProblem existing = existingOpt.get();
        Long id = existing.getId();

        String updatedTitle = title != null && !title.isBlank() ? title : existing.getTitle();
        String updatedPlatform = parameters.get("platform") != null ? parameters.get("platform").toString() : existing.getPlatform();
        String updatedTopic = parameters.get("topic") != null ? parameters.get("topic").toString() : existing.getTopic();
        String updatedNotes = parameters.get("notes") != null ? parameters.get("notes").toString() : existing.getNotes();
        String updatedUrl = parameters.get("problemUrl") != null ? parameters.get("problemUrl").toString() : existing.getProblemUrl();

        Difficulty difficulty = existing.getDifficulty();
        if (parameters.get("difficulty") != null) {
            try {
                difficulty = Difficulty.valueOf(parameters.get("difficulty").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        ProblemStatus status = existing.getStatus();
        if (parameters.get("status") != null) {
            try {
                status = ProblemStatus.valueOf(parameters.get("status").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        Integer timeTaken = existing.getTimeTaken();
        if (parameters.get("timeTaken") != null) {
            try {
                timeTaken = Integer.parseInt(parameters.get("timeTaken").toString());
            } catch (Exception ignored) {}
        }

        LocalDate dateSolved = status == ProblemStatus.SOLVED && existing.getDateSolved() == null ? LocalDate.now() : existing.getDateSolved();

        UpdateDsaProblemRequest updateRequest = new UpdateDsaProblemRequest(
                updatedTitle,
                updatedPlatform,
                updatedUrl,
                updatedTopic,
                difficulty,
                status,
                dateSolved,
                timeTaken,
                updatedNotes,
                existing.getRevisionDate()
        );

        try {
            DsaProblemResponse response = dsaProblemService.update(id, userId, updateRequest);
            return ToolExecutionResult.success("Updated DSA problem '" + response.title() + "' (Status: " + response.status() + ").", response);
        } catch (Exception e) {
            log.error("Failed to update DSA problem: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to update DSA problem: " + e.getMessage());
        }
    }
}
