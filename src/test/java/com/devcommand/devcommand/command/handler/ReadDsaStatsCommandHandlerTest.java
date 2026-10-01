package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadDsaStatsCommandHandlerTest {

    @Mock
    private DsaProblemRepository dsaProblemRepository;

    private ReadDsaStatsCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReadDsaStatsCommandHandler(dsaProblemRepository);
    }

    @Test
    void supports_ReturnsTrueForReadDsaStats() {
        assertTrue(handler.supports(CommandType.READ_DSA_STATS));
        assertFalse(handler.supports(CommandType.CREATE_DSA_PROBLEM));
    }

    @Test
    void handle_ReturnsStatsSuccessfully() {
        Long userId = 99L;
        Command command = new Command(CommandType.READ_DSA_STATS, userId, new CommandParameters(Collections.emptyMap()));

        DsaProblemRepository.DsaAnalyticsProjection mockStats = mock(DsaProblemRepository.DsaAnalyticsProjection.class);
        when(mockStats.getTotal()).thenReturn(10L);
        when(mockStats.getSolvedCount()).thenReturn(10L);
        when(mockStats.getEasyCount()).thenReturn(5L);
        when(mockStats.getMediumCount()).thenReturn(3L);
        when(mockStats.getHardCount()).thenReturn(2L);
        when(mockStats.getSolvedToday()).thenReturn(1L);
        when(mockStats.getSolvedThisWeek()).thenReturn(2L);
        when(mockStats.getSolvedThisMonth()).thenReturn(4L);
        when(mockStats.getRevisionCount()).thenReturn(0L);
        when(mockStats.getMasteredCount()).thenReturn(0L);

        when(dsaProblemRepository.getDsaAnalyticsByUserId(eq(userId), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockStats);

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("Total Solved: 10"));
        assertTrue(result.message().contains("Easy: 5"));
        assertTrue(result.message().contains("Medium: 3"));
        assertTrue(result.message().contains("Hard: 2"));

        verify(dsaProblemRepository).getDsaAnalyticsByUserId(eq(userId), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
    }
}
