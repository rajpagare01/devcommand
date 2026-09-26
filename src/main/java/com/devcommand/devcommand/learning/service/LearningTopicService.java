package com.devcommand.devcommand.learning.service;

import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.dto.LearningSummary;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.mapper.LearningTopicMapper;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.learning.repository.LearningTopicSpecifications;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All Learning Tracker business logic lives here, mirroring
 * DsaProblemService/DailyTaskService/JobApplicationService's shape.
 *
 * Every method takes the caller's user id as an explicit parameter rather
 * than reading SecurityContextHolder itself - same rationale as the other
 * three modules: it's what keeps this testable with plain Mockito, and
 * what would let a future WhatsApp command parser ("I studied Docker for
 * 2 hours") resolve a phone number to a userId and call
 * updateProgress(...)/complete(...) directly, no service changes needed.
 */
@Service
@RequiredArgsConstructor
public class LearningTopicService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "progress", "hoursSpent", "createdAt", "technology", "topic"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final LearningTopicRepository learningTopicRepository;
    private final UserRepository userRepository;
    private final LearningTopicMapper mapper;

    @Transactional
    public LearningTopicResponse create(CreateLearningTopicRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);
        LearningTopic topic = mapper.toEntity(request, owner);
        LearningTopic saved = learningTopicRepository.save(topic);
        return mapper.toResponse(saved);
    }

    public Page<LearningTopicResponse> getAll(
            Long userId,
            String technology,
            String statusParam,
            Integer progress,
            String search,
            Integer page,
            Integer size,
            String sortBy,
            String direction
    ) {
        Specification<LearningTopic> spec = buildSpecification(userId, technology, statusParam, progress, search);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return learningTopicRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public LearningTopicResponse getById(Long id, Long userId) {
        return mapper.toResponse(findOwned(id, userId));
    }

    @Transactional
    public LearningTopicResponse update(Long id, Long userId, UpdateLearningTopicRequest request) {
        LearningTopic topic = findOwned(id, userId);
        mapper.applyUpdate(topic, request);
        return mapper.toResponse(topic); // managed entity - flushed automatically at commit
    }

    @Transactional
    public void delete(Long id, Long userId) {
        LearningTopic topic = findOwned(id, userId);
        learningTopicRepository.delete(topic);
    }

    /**
     * Applies the auto-transition rules from the spec, in priority order:
     * 1. progress == 100 -> status = COMPLETED, always. Reaching 100% is an
     *    unambiguous "I finished this" signal, strong enough to end even
     *    an ON_HOLD pause without a separate explicit action.
     * 2. Otherwise, if status is currently ON_HOLD, status is left alone.
     *    This is the "don't override an explicit ON_HOLD" protection the
     *    spec calls for - it guards specifically against rule 3 silently
     *    un-pausing a topic just because a progress update came in.
     * 3. Otherwise, if progress > 0 and status is NOT_STARTED, status
     *    bumps to IN_PROGRESS - logging any real progress on an untouched
     *    topic implies it's now underway.
     * 4. In every other case (e.g. progress dropped back towards 0, or
     *    status was already IN_PROGRESS/COMPLETED), status is left as-is;
     *    nothing in the spec calls for any other automatic change.
     */
    @Transactional
    public LearningTopicResponse updateProgress(Long id, Long userId, ProgressUpdateRequest request) {
        LearningTopic topic = findOwned(id, userId);
        topic.setProgress(request.progress());

        if (request.progress() == 100) {
            topic.setStatus(LearningStatus.COMPLETED);
        } else if (topic.getStatus() != LearningStatus.ON_HOLD
                && request.progress() > 0
                && topic.getStatus() == LearningStatus.NOT_STARTED) {
            topic.setStatus(LearningStatus.IN_PROGRESS);
        }

        return mapper.toResponse(topic);
    }

    /** Explicit completion endpoint - unlike updateProgress, this is allowed to override ON_HOLD. */
    @Transactional
    public LearningTopicResponse complete(Long id, Long userId) {
        LearningTopic topic = findOwned(id, userId);
        topic.setProgress(100);
        topic.setStatus(LearningStatus.COMPLETED);
        return mapper.toResponse(topic);
    }

    public List<LearningTopicResponse> completedTopics(Long userId) {
        Specification<LearningTopic> spec = Specification.allOf(
                LearningTopicSpecifications.ownerIs(userId),
                LearningTopicSpecifications.statusEquals(LearningStatus.COMPLETED)
        );
        return learningTopicRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(mapper::toResponse).toList();
    }

    public List<LearningTopicResponse> inProgressTopics(Long userId) {
        Specification<LearningTopic> spec = Specification.allOf(
                LearningTopicSpecifications.ownerIs(userId),
                LearningTopicSpecifications.statusEquals(LearningStatus.IN_PROGRESS)
        );
        return learningTopicRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "progress"))
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Not exposed by any controller/endpoint - no /api/analytics route
     * exists yet, per the spec. Computed on the fly from the caller's
     * existing topics (no new table, no persisted aggregate), so a future
     * dashboard/analytics module can call this directly instead of
     * re-deriving the same numbers.
     */
    public LearningSummary getSummary(Long userId) {
        List<LearningTopic> topics = learningTopicRepository.findAll(LearningTopicSpecifications.ownerIs(userId));

        long total = topics.size();
        long completed = topics.stream().filter(t -> t.getStatus() == LearningStatus.COMPLETED).count();
        long inProgress = topics.stream().filter(t -> t.getStatus() == LearningStatus.IN_PROGRESS).count();
        double totalHours = topics.stream().mapToDouble(t -> t.getHoursSpent() != null ? t.getHoursSpent() : 0.0).sum();
        double averageProgress = total == 0
                ? 0.0
                : topics.stream().mapToInt(LearningTopic::getProgress).average().orElse(0.0);

        return new LearningSummary(total, completed, inProgress, totalHours, averageProgress);
    }

    // ---- helpers ---------------------------------------------------------

    private LearningTopic findOwned(Long id, Long userId) {
        return learningTopicRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning topic not found: " + id));
    }

    private Specification<LearningTopic> buildSpecification(
            Long userId, String technology, String statusParam, Integer progress, String search
    ) {
        List<Specification<LearningTopic>> specs = new ArrayList<>();
        specs.add(LearningTopicSpecifications.ownerIs(userId));

        if (technology != null && !technology.isBlank()) {
            specs.add(LearningTopicSpecifications.technologyEquals(technology));
        }
        if (statusParam != null && !statusParam.isBlank()) {
            specs.add(LearningTopicSpecifications.statusEquals(parseStatus(statusParam)));
        }
        if (progress != null) {
            specs.add(LearningTopicSpecifications.progressEquals(progress));
        }
        if (search != null && !search.isBlank()) {
            specs.add(LearningTopicSpecifications.searchKeyword(search));
        }

        return Specification.allOf(specs);
    }

    private LearningStatus parseStatus(String rawValue) {
        try {
            return LearningStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + rawValue);
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
