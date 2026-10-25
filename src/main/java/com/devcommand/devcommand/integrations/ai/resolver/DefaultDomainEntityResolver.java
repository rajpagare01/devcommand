package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.dsa.catalog.entity.DsaProblemCatalogEntry;
import com.devcommand.devcommand.dsa.catalog.service.DsaProblemCatalogService;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.mapper.JobApplicationMapper;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.mapper.LearningTopicMapper;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.mapper.ProjectMapper;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.projects.repository.ProjectSpecifications;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDomainEntityResolver implements DomainEntityResolver {

    private static final Set<String> CONTEXTUAL_PRONOUNS = Set.of(
            "it", "that", "this", "that one", "this one",
            "that task", "the task", "the previous task", "previous task",
            "that problem", "the problem", "the previous problem", "previous problem",
            "that project", "the project", "the previous project", "previous project",
            "that application", "the application", "that company", "the company",
            "that topic", "the topic", "the previous topic"
    );

    private final DsaProblemCatalogService dsaCatalogService;
    private final DailyTaskService dailyTaskService;
    private final ConversationContextService contextService;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;
    private final LearningTopicRepository learningTopicRepository;
    private final LearningTopicMapper learningTopicMapper;

    @Override
    public DsaProblemResolution resolveDsaProblem(String rawTitle, String rawPlatform, Difficulty explicitDifficulty) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return DsaProblemResolution.ambiguous("Please specify the title of the DSA problem you solved.");
        }

        String cleanedTitle = cleanTitle(rawTitle);
        String resolvedPlatform = normalizePlatform(rawPlatform);

        // 1. Authoritative exact lookup in catalog
        Optional<DsaProblemCatalogEntry> exactMatch = dsaCatalogService.findExactMatch(cleanedTitle);
        if (exactMatch.isPresent()) {
            DsaProblemCatalogEntry entry = exactMatch.get();
            Difficulty finalDifficulty = explicitDifficulty != null ? explicitDifficulty : entry.getDifficulty();
            String finalPlatform = (rawPlatform != null && !rawPlatform.isBlank()) ? resolvedPlatform : entry.getPlatform();
            return DsaProblemResolution.resolved(
                    entry.getTitle(),
                    finalPlatform,
                    finalDifficulty,
                    entry.getTopic(),
                    entry.getProblemUrl()
            );
        }

        // 2. Fuzzy / partial search in catalog
        List<DsaProblemCatalogEntry> candidates = dsaCatalogService.searchSimilar(cleanedTitle);
        if (candidates.size() == 1) {
            DsaProblemCatalogEntry entry = candidates.get(0);
            Difficulty finalDifficulty = explicitDifficulty != null ? explicitDifficulty : entry.getDifficulty();
            String finalPlatform = (rawPlatform != null && !rawPlatform.isBlank()) ? resolvedPlatform : entry.getPlatform();
            return DsaProblemResolution.resolved(
                    entry.getTitle(),
                    finalPlatform,
                    finalDifficulty,
                    entry.getTopic(),
                    entry.getProblemUrl()
            );
        }

        if (candidates.size() > 1) {
            // Check if one candidate is an exact normalized match
            String targetNormalized = DsaProblemCatalogService.normalizeTitle(cleanedTitle);
            List<DsaProblemCatalogEntry> exactNormalized = candidates.stream()
                    .filter(c -> c.getNormalizedTitle().equalsIgnoreCase(targetNormalized))
                    .toList();

            if (exactNormalized.size() == 1) {
                DsaProblemCatalogEntry entry = exactNormalized.get(0);
                Difficulty finalDifficulty = explicitDifficulty != null ? explicitDifficulty : entry.getDifficulty();
                String finalPlatform = (rawPlatform != null && !rawPlatform.isBlank()) ? resolvedPlatform : entry.getPlatform();
                return DsaProblemResolution.resolved(
                        entry.getTitle(),
                        finalPlatform,
                        finalDifficulty,
                        entry.getTopic(),
                        entry.getProblemUrl()
                );
            }

            String candidateTitles = candidates.stream()
                    .map(DsaProblemCatalogEntry::getTitle)
                    .limit(4)
                    .collect(Collectors.joining("', '"));

            return DsaProblemResolution.ambiguous(
                    "I found multiple possible matches for '" + rawTitle + "': '" + candidateTitles + "'. Which one did you mean?"
            );
        }

        // 3. Not found in catalog: allow explicit user input or ask for difficulty
        if (explicitDifficulty != null) {
            return DsaProblemResolution.resolved(
                    cleanedTitle,
                    resolvedPlatform != null ? resolvedPlatform : "LEETCODE",
                    explicitDifficulty,
                    "Uncategorized",
                    null
            );
        }

        return DsaProblemResolution.unknown(
                cleanedTitle,
                "I couldn't identify the difficulty for '" + cleanedTitle + "'. What difficulty level (Easy, Medium, or Hard) should I set?"
        );
    }

    @Override
    public TaskResolution resolveTask(Long userId, String rawReference, String chatId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated userId is required for task resolution");
        }

        // Check contextual reference (e.g. "it", "that task", null or blank)
        if (isContextualReference(rawReference)) {
            Optional<ConversationContext> optContext = (chatId != null && !chatId.isBlank())
                    ? contextService.getContext(userId, chatId)
                    : Optional.empty();

            if (optContext.isEmpty()) {
                optContext = contextService.getLatestContext(userId);
            }

            if (optContext.isPresent() && optContext.get().getContextType() == ContextType.LAST_TASK) {
                Map<String, Object> data = optContext.get().getContextData();
                Object idObj = data != null ? data.get("id") : null;
                if (idObj != null) {
                    try {
                        Long taskId = Long.parseLong(idObj.toString());
                        DailyTaskResponse task = dailyTaskService.getById(taskId, userId);
                        return TaskResolution.resolved(taskId, task.title(), task);
                    } catch (Exception e) {
                        log.warn("Failed to load last task {} for user {}", idObj, userId, e);
                    }
                }
            }
            return TaskResolution.ambiguous("I'm not sure which task you're referring to. Could you specify the task title or ID?");
        }

        // Check numeric ID
        try {
            Long taskId = Long.parseLong(rawReference.trim());
            DailyTaskResponse task = dailyTaskService.getById(taskId, userId);
            return TaskResolution.resolved(taskId, task.title(), task);
        } catch (NumberFormatException ignored) {
        } catch (Exception e) {
            return TaskResolution.notFound("Task ID " + rawReference + " not found or you don't have access.");
        }

        // Search pending tasks by title
        List<DailyTaskResponse> matches = dailyTaskService.searchPendingTasks(userId, rawReference.trim());
        if (matches.size() == 1) {
            DailyTaskResponse match = matches.get(0);
            return TaskResolution.resolved(match.id(), match.title(), match);
        }

        if (matches.size() > 1) {
            String titles = matches.stream().map(DailyTaskResponse::title).limit(3).collect(Collectors.joining("', '"));
            return TaskResolution.ambiguous("Multiple pending tasks match '" + rawReference + "': '" + titles + "'. Please specify which one or use the task ID.");
        }

        return TaskResolution.notFound("No pending task found matching '" + rawReference + "'.");
    }

    @Override
    public ProjectResolution resolveProject(Long userId, String rawReference, String chatId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated userId is required for project resolution");
        }

        if (isContextualReference(rawReference)) {
            Optional<ConversationContext> optContext = (chatId != null && !chatId.isBlank())
                    ? contextService.getContext(userId, chatId)
                    : Optional.empty();

            if (optContext.isEmpty()) {
                optContext = contextService.getLatestContext(userId);
            }

            if (optContext.isPresent() && optContext.get().getContextType() == ContextType.LAST_PROJECT) {
                Map<String, Object> data = optContext.get().getContextData();
                Object idObj = data != null ? data.get("id") : null;
                if (idObj != null) {
                    try {
                        Long projectId = Long.parseLong(idObj.toString());
                        Project project = projectRepository.findByIdAndUserId(projectId, userId).orElse(null);
                        if (project != null) {
                            return ProjectResolution.resolved(projectId, project.getName(), projectMapper.toResponse(project));
                        }
                    } catch (Exception ignored) {}
                }
            }
            return ProjectResolution.ambiguous("Which project are you referring to?");
        }

        // Search projects by name
        Specification<Project> spec = Specification.allOf(
                ProjectSpecifications.ownerIs(userId),
                (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + rawReference.trim().toLowerCase() + "%")
        );
        List<Project> matches = projectRepository.findAll(spec);
        if (matches.size() == 1) {
            Project p = matches.get(0);
            return ProjectResolution.resolved(p.getId(), p.getName(), projectMapper.toResponse(p));
        }
        if (matches.size() > 1) {
            return ProjectResolution.ambiguous("Multiple projects match '" + rawReference + "'. Which one did you mean?");
        }

        return ProjectResolution.notFound("No project found matching '" + rawReference + "'.");
    }

    @Override
    public JobApplicationResolution resolveJobApplication(Long userId, String rawReference, String chatId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated userId is required for job application resolution");
        }

        if (isContextualReference(rawReference)) {
            Optional<ConversationContext> optContext = (chatId != null && !chatId.isBlank())
                    ? contextService.getContext(userId, chatId)
                    : Optional.empty();

            if (optContext.isEmpty()) {
                optContext = contextService.getLatestContext(userId);
            }

            if (optContext.isPresent() && optContext.get().getContextType() == ContextType.LAST_JOB_APPLICATION) {
                Map<String, Object> data = optContext.get().getContextData();
                Object idObj = data != null ? data.get("id") : null;
                if (idObj != null) {
                    try {
                        Long appId = Long.parseLong(idObj.toString());
                        JobApplication app = jobApplicationRepository.findByIdAndUserId(appId, userId).orElse(null);
                        if (app != null) {
                            return JobApplicationResolution.resolved(appId, app.getCompany(), jobApplicationMapper.toResponse(app));
                        }
                    } catch (Exception ignored) {}
                }
            }
            return JobApplicationResolution.ambiguous("Which job application are you referring to?");
        }

        List<JobApplication> matches = jobApplicationRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("user").get("id"), userId),
                        cb.like(cb.lower(root.get("company")), "%" + rawReference.trim().toLowerCase() + "%")
                )
        );

        if (matches.size() == 1) {
            JobApplication app = matches.get(0);
            return JobApplicationResolution.resolved(app.getId(), app.getCompany(), jobApplicationMapper.toResponse(app));
        }
        if (matches.size() > 1) {
            return JobApplicationResolution.ambiguous("Multiple job applications match '" + rawReference + "'. Which company did you mean?");
        }

        return JobApplicationResolution.notFound("No job application found for '" + rawReference + "'.");
    }

    @Override
    public LearningTopicResolution resolveLearningTopic(Long userId, String rawReference, String chatId) {
        if (userId == null) {
            throw new IllegalArgumentException("Authenticated userId is required for learning topic resolution");
        }

        if (isContextualReference(rawReference)) {
            Optional<ConversationContext> optContext = (chatId != null && !chatId.isBlank())
                    ? contextService.getContext(userId, chatId)
                    : Optional.empty();

            if (optContext.isEmpty()) {
                optContext = contextService.getLatestContext(userId);
            }

            if (optContext.isPresent() && optContext.get().getContextType() == ContextType.LAST_LEARNING_TOPIC) {
                Map<String, Object> data = optContext.get().getContextData();
                Object topicObj = data != null ? data.get("topic") : null;
                if (topicObj != null) {
                    List<LearningTopic> topics = learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, topicObj.toString());
                    if (!topics.isEmpty()) {
                        LearningTopic t = topics.get(0);
                        return LearningTopicResolution.resolved(t.getId(), t.getTopic(), learningTopicMapper.toResponse(t));
                    }
                }
            }
            return LearningTopicResolution.ambiguous("Which learning topic are you referring to?");
        }

        List<LearningTopic> topics = learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, rawReference.trim());
        if (topics.size() == 1) {
            LearningTopic t = topics.get(0);
            return LearningTopicResolution.resolved(t.getId(), t.getTopic(), learningTopicMapper.toResponse(t));
        }
        if (topics.size() > 1) {
            return LearningTopicResolution.ambiguous("Multiple topics match '" + rawReference + "'. Which one did you mean?");
        }

        return LearningTopicResolution.notFound("No learning topic found matching '" + rawReference + "'.");
    }

    private boolean isContextualReference(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String clean = text.trim().toLowerCase();
        return CONTEXTUAL_PRONOUNS.contains(clean);
    }

    private String cleanTitle(String rawTitle) {
        String clean = rawTitle.trim();
        if (clean.toLowerCase().startsWith("the ")) {
            clean = clean.substring(4).trim();
        }
        return clean;
    }

    private String normalizePlatform(String rawPlatform) {
        if (rawPlatform == null || rawPlatform.isBlank()) {
            return "LEETCODE";
        }
        String p = rawPlatform.trim().toUpperCase();
        if ("LC".equals(p) || "LEETCODE".equals(p)) {
            return "LEETCODE";
        }
        if ("HR".equals(p) || "HACKERRANK".equals(p)) {
            return "HACKERRANK";
        }
        if ("GFG".equals(p) || "GEEKSFORGEEKS".equals(p)) {
            return "GEEKSFORGEEKS";
        }
        return p;
    }
}
