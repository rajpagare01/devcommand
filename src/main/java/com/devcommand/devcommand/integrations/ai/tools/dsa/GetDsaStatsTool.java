package com.devcommand.devcommand.integrations.ai.tools.dsa;

import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetDsaStatsTool implements AiTool {

    private final DsaProblemRepository dsaProblemRepository;

    @Override
    public String getName() {
        return "get_dsa_stats";
    }

    @Override
    public String getDescription() {
        return "Retrieves overall and period-based DSA statistics (total solved, difficulty breakdown, today, this week, this month).";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of("Fetches DSA progress statistics for the current user");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);

        DsaProblemRepository.DsaAnalyticsProjection stats = dsaProblemRepository.getDsaAnalyticsByUserId(
                userId, today, startOfWeek, startOfMonth
        );

        if (stats == null || (stats.getTotal() != null && stats.getTotal() == 0)) {
            return ToolExecutionResult.success("You haven't tracked any DSA problems yet. Keep pushing!", Map.of("total", 0));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", stats.getTotal() != null ? stats.getTotal() : 0);
        data.put("solved", stats.getSolvedCount() != null ? stats.getSolvedCount() : 0);
        data.put("easy", stats.getEasyCount() != null ? stats.getEasyCount() : 0);
        data.put("medium", stats.getMediumCount() != null ? stats.getMediumCount() : 0);
        data.put("hard", stats.getHardCount() != null ? stats.getHardCount() : 0);
        data.put("solvedToday", stats.getSolvedToday() != null ? stats.getSolvedToday() : 0);
        data.put("solvedThisWeek", stats.getSolvedThisWeek() != null ? stats.getSolvedThisWeek() : 0);
        data.put("solvedThisMonth", stats.getSolvedThisMonth() != null ? stats.getSolvedThisMonth() : 0);
        data.put("revisionCount", stats.getRevisionCount() != null ? stats.getRevisionCount() : 0);
        data.put("masteredCount", stats.getMasteredCount() != null ? stats.getMasteredCount() : 0);

        String summary = String.format("DSA Stats: %d solved (%d Easy, %d Med, %d Hard). Solved today: %d, this week: %d.",
                data.get("solved"), data.get("easy"), data.get("medium"), data.get("hard"),
                data.get("solvedToday"), data.get("solvedThisWeek"));

        return ToolExecutionResult.success(summary, data);
    }
}
