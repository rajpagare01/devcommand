package com.devcommand.devcommand.tasks.service;

import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.mapper.DailyTaskMapper;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.tasks.repository.DailyTaskSpecifications;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All Daily Tasks business logic lives here, mirroring DsaProblemService's
 * shape from the DSA module.
 *
 * Every method takes the caller's user id as an explicit parameter rather
 * than reading SecurityContextHolder itself - this is also what makes the
 * class a natural fit for the "same service, multiple front doors" design
 * called for in the brief: a future WhatsApp webhook's message parser would
 * resolve a phone number to a userId and call create(...)/complete(...)
 * exactly like DailyTaskController does today, with no service changes
 * needed. No WhatsApp classes exist yet - this is just why the method
 * signatures already look the way they do.
 */
@Service
@RequiredArgsConstructor
public class DailyTaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "dueDate", "priority", "title", "status"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final DailyTaskRepository dailyTaskRepository;
    private final UserRepository userRepository;
    private final DailyTaskMapper mapper;

    @Transactional
    public DailyTaskResponse create(CreateDailyTaskRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);
        DailyTask task = mapper.toEntity(request, owner);
        DailyTask saved = dailyTaskRepository.save(task);
        return mapper.toResponse(saved);
    }

    public Page<DailyTaskResponse> getAll(
            Long userId,
            String categoryParam,
            String priorityParam,
            String statusParam,
            LocalDate dueDate,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        Specification<DailyTask> spec = buildSpecification(userId, categoryParam, priorityParam, statusParam, dueDate);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return dailyTaskRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public DailyTaskResponse getById(Long id, Long userId) {
        return mapper.toResponse(findOwned(id, userId));
    }

    @Transactional
    public DailyTaskResponse update(Long id, Long userId, UpdateDailyTaskRequest request) {
        DailyTask task = findOwned(id, userId);
        mapper.applyUpdate(task, request);
        return mapper.toResponse(task); // managed entity - flushed automatically at commit
    }

    @Transactional
    public void delete(Long id, Long userId) {
        DailyTask task = findOwned(id, userId);
        dailyTaskRepository.delete(task);
    }

    @Transactional
    public DailyTaskResponse complete(Long id, Long userId) {
        DailyTask task = findOwned(id, userId);
        // Idempotent: completing an already-completed task just returns it
        // as-is rather than overwriting completedAt with a new timestamp or
        // creating a second record.
        if (task.getStatus() != DailyTaskStatus.COMPLETED) {
            task.setStatus(DailyTaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
        }
        return mapper.toResponse(task);
    }

    @Transactional
    public DailyTaskResponse start(Long id, Long userId) {
        DailyTask task = findOwned(id, userId);
        task.setStatus(DailyTaskStatus.IN_PROGRESS);
        return mapper.toResponse(task);
    }

    /** Today's tasks for the dashboard: dueDate == today, any status. */
    public List<DailyTaskResponse> today(Long userId) {
        Specification<DailyTask> spec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                DailyTaskSpecifications.dueDateEquals(LocalDate.now())
        );
        return dailyTaskRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dueDate"))
                .stream().map(mapper::toResponse).toList();
    }

    /** Incomplete tasks with a future due date, soonest first. */
    public List<DailyTaskResponse> upcoming(Long userId) {
        Specification<DailyTask> spec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                DailyTaskSpecifications.statusNot(DailyTaskStatus.COMPLETED),
                DailyTaskSpecifications.dueDateAfter(LocalDate.now())
        );
        return dailyTaskRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dueDate"))
                .stream().map(mapper::toResponse).toList();
    }

    /** All incomplete (pending) tasks, returning a paginated list. */
    public Page<DailyTaskResponse> getPending(Long userId, Pageable pageable) {
        Specification<DailyTask> spec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                DailyTaskSpecifications.statusNot(DailyTaskStatus.COMPLETED)
        );
        return dailyTaskRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    /** Completed tasks, most recently completed first. */
    public List<DailyTaskResponse> completed(Long userId) {
        Specification<DailyTask> spec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                DailyTaskSpecifications.statusEquals(DailyTaskStatus.COMPLETED)
        );
        return dailyTaskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "completedAt"))
                .stream().map(mapper::toResponse).toList();
    }

    // ---- helpers ---------------------------------------------------------

    public List<DailyTaskResponse> searchPendingTasks(Long userId, String keyword) {
        Specification<DailyTask> spec = Specification.allOf(
                DailyTaskSpecifications.ownerIs(userId),
                DailyTaskSpecifications.statusNot(DailyTaskStatus.COMPLETED),
                DailyTaskSpecifications.titleContainsIgnoreCase(keyword)
        );
        return dailyTaskRepository.findAll(spec).stream().map(mapper::toResponse).toList();
    }

    /** Ownership-safe single-record lookup used by every by-id operation. */
    private DailyTask findOwned(Long id, Long userId) {
        return dailyTaskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private Specification<DailyTask> buildSpecification(
            Long userId, String categoryParam, String priorityParam, String statusParam, LocalDate dueDate
    ) {
        List<Specification<DailyTask>> specs = new ArrayList<>();
        specs.add(DailyTaskSpecifications.ownerIs(userId));

        if (categoryParam != null && !categoryParam.isBlank()) {
            specs.add(DailyTaskSpecifications.categoryEquals(parseEnum(TaskCategory.class, categoryParam, "category")));
        }
        if (priorityParam != null && !priorityParam.isBlank()) {
            specs.add(DailyTaskSpecifications.priorityEquals(parseEnum(TaskPriority.class, priorityParam, "priority")));
        }
        if (statusParam != null && !statusParam.isBlank()) {
            specs.add(DailyTaskSpecifications.statusEquals(parseEnum(DailyTaskStatus.class, statusParam, "status")));
        }
        if (dueDate != null) {
            specs.add(DailyTaskSpecifications.dueDateEquals(dueDate));
        }

        return Specification.allOf(specs);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldName) {
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + fieldName + " value: " + rawValue);
        }
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int resolvedPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int resolvedSize = (size != null && size > 0) ? size : DEFAULT_SIZE;

        String resolvedSortField = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy))
                ? sortBy
                : DEFAULT_SORT_FIELD;

        Sort.Direction resolvedDirection = ("asc".equalsIgnoreCase(direction))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC; // default: desc (newest/highest first)

        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(resolvedDirection, resolvedSortField));
    }
}
