package com.devcommand.devcommand.integrations.ai.tools.dsa;

import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetDsaProblemTool implements AiTool {

    private final DsaProblemService dsaProblemService;
    private final DsaProblemRepository dsaProblemRepository;
    private final DsaProblemMapper mapper;

    @Override
    public String getName() {
        return "get_dsa_problem";
    }

    @Override
    public String getDescription() {
        return "Fetches details of a tracked DSA problem by its ID or title.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Gets a DSA problem by ID or title",
                ToolParameter.optional("id", "integer", "ID of the DSA problem"),
                ToolParameter.optional("title", "string", "Title of the problem")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        if (parameters.get("id") != null) {
            try {
                Long id = Long.parseLong(parameters.get("id").toString());
                DsaProblemResponse resp = dsaProblemService.getById(id, userId);
                return ToolExecutionResult.success("Found DSA problem '" + resp.title() + "'.", resp);
            } catch (Exception e) {
                return ToolExecutionResult.failure("Problem not found with id: " + parameters.get("id"));
            }
        }

        String title = parameters.get("title") != null ? parameters.get("title").toString() : null;
        if (title != null && !title.isBlank()) {
            Optional<DsaProblem> problemOpt = dsaProblemRepository.findFirstByUserIdAndTitleIgnoreCase(userId, title);
            if (problemOpt.isPresent()) {
                DsaProblemResponse resp = mapper.toResponse(problemOpt.get());
                return ToolExecutionResult.success("Found DSA problem '" + resp.title() + "'.", resp);
            }
            return ToolExecutionResult.failure("DSA problem not found with title: " + title);
        }

        return ToolExecutionResult.failure("Either problem ID or title must be provided.");
    }
}
