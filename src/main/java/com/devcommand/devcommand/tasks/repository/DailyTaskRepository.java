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
}
