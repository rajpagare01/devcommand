package com.devcommand.devcommand.tasks.repository;

import com.devcommand.devcommand.tasks.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JpaSpecificationExecutor backs the combinable, optional filters on
 * GET /api/tasks (status/priority/category/dueDate), plus the today/
 * upcoming/completed convenience endpoints, without a derived-query method
 * per combination.
 *
 * findByIdAndUserId is the ownership-safe alternative to findById(id) -
 * used for every single-record operation (get one/update/delete/complete/
 * start), matching the pattern already established by
 * DsaProblemRepository.
 */
public interface DailyTaskRepository extends JpaRepository<DailyTask, Long>, JpaSpecificationExecutor<DailyTask> {

    Optional<DailyTask> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, com.devcommand.devcommand.tasks.entity.DailyTaskStatus status);
    long countByUserIdAndDueDate(Long userId, java.time.LocalDate dueDate);
    long countByUserIdAndDueDateBeforeAndStatusNot(Long userId, java.time.LocalDate dueDate, com.devcommand.devcommand.tasks.entity.DailyTaskStatus status);
    long countByUserIdAndStatusAndCompletedAtBetween(Long userId, com.devcommand.devcommand.tasks.entity.DailyTaskStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    interface TasksAnalyticsProjection {
        Long getTotalTasks();
        Long getTodoTasks();
        Long getInProgressTasks();
        Long getCompletedTasks();
        Long getTasksDueToday();
        Long getOverdueTasks();
        Long getCompletedToday();
    }

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "COUNT(t) as totalTasks, " +
           "SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) as todoTasks, " +
           "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgressTasks, " +
           "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedTasks, " +
           "SUM(CASE WHEN t.dueDate = :today THEN 1 ELSE 0 END) as tasksDueToday, " +
           "SUM(CASE WHEN t.dueDate < :today AND t.status <> 'COMPLETED' THEN 1 ELSE 0 END) as overdueTasks, " +
           "SUM(CASE WHEN t.status = 'COMPLETED' AND t.completedAt BETWEEN :startOfDay AND :endOfDay THEN 1 ELSE 0 END) as completedToday " +
           "FROM DailyTask t WHERE t.user.id = :userId")
    TasksAnalyticsProjection getTasksAnalyticsByUserId(
        @org.springframework.data.repository.query.Param("userId") Long userId, 
        @org.springframework.data.repository.query.Param("today") java.time.LocalDate today, 
        @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay, 
        @org.springframework.data.repository.query.Param("endOfDay") java.time.LocalDateTime endOfDay
    );

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "COUNT(t) as total, " +
           "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN t.status IN ('TODO', 'IN_PROGRESS') THEN 1 ELSE 0 END) as pending " +
           "FROM DailyTask t WHERE t.user.id = :userId")
    TasksOverviewProjection getTasksOverviewByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    interface TasksOverviewProjection {
        Long getTotal();
        Long getCompleted();
        Long getPending();
    }
}
