package com.devcommand.devcommand.learning.mapper;

import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.user.entity.User;
import org.springframework.stereotype.Component;

/** Plain hand-written mapper, matching the pattern established by the DSA/Tasks/Jobs modules. */
@Component
public class LearningTopicMapper {

    public LearningTopic toEntity(CreateLearningTopicRequest request, User owner) {
        return LearningTopic.builder()
                .technology(request.technology())
                .topic(request.topic())
                .progress(request.progress())
                .status(request.status())
                .hoursSpent(request.hoursSpent())
                .resourceUrl(request.resourceUrl())
                .notes(request.notes())
                .user(owner)
                .build();
    }

    /** Full-replace update onto an already-loaded, owned entity. Never touches user. */
    public void applyUpdate(LearningTopic topic, UpdateLearningTopicRequest request) {
        topic.setTechnology(request.technology());
        topic.setTopic(request.topic());
        topic.setProgress(request.progress());
        topic.setStatus(request.status());
        topic.setHoursSpent(request.hoursSpent());
        topic.setResourceUrl(request.resourceUrl());
        topic.setNotes(request.notes());
    }

    public LearningTopicResponse toResponse(LearningTopic topic) {
        return new LearningTopicResponse(
                topic.getId(),
                topic.getTechnology(),
                topic.getTopic(),
                topic.getProgress(),
                topic.getStatus(),
                topic.getHoursSpent(),
                topic.getResourceUrl(),
                topic.getNotes(),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}
