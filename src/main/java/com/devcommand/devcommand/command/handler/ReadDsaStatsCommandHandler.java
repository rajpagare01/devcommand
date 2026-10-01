package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;

@Component
@RequiredArgsConstructor
public class ReadDsaStatsCommandHandler implements CommandHandler {

    private final DsaProblemRepository dsaProblemRepository;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.READ_DSA_STATS;
    }

    @Override
    public CommandResult handle(Command command) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        DsaProblemRepository.DsaAnalyticsProjection stats = dsaProblemRepository.getDsaAnalyticsByUserId(
                command.userId(), today, startOfWeek, startOfMonth);

        if (stats == null || (stats.getTotal() != null && stats.getTotal() == 0)) {
            return CommandResult.success("You haven't tracked any DSA problems yet. Keep pushing!");
        }

        String msg = String.format("""
            📊 *DSA Progress Stats*
            
            Total Solved: %d
            - Easy: %d
            - Medium: %d
            - Hard: %d
            
            📈 *Recent Activity*
            - Today: %d
            - This Week: %d
            - This Month: %d
            
            Needs Revision: %d
            Mastered: %d
            """,
            stats.getSolvedCount() != null ? stats.getSolvedCount() : 0,
            stats.getEasyCount() != null ? stats.getEasyCount() : 0,
            stats.getMediumCount() != null ? stats.getMediumCount() : 0,
            stats.getHardCount() != null ? stats.getHardCount() : 0,
            stats.getSolvedToday() != null ? stats.getSolvedToday() : 0,
            stats.getSolvedThisWeek() != null ? stats.getSolvedThisWeek() : 0,
            stats.getSolvedThisMonth() != null ? stats.getSolvedThisMonth() : 0,
            stats.getRevisionCount() != null ? stats.getRevisionCount() : 0,
            stats.getMasteredCount() != null ? stats.getMasteredCount() : 0
        );

        return CommandResult.success(msg.trim());
    }
}
