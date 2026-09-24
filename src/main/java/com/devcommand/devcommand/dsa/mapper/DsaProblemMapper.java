package com.devcommand.devcommand.dsa.mapper;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.dto.UpdateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Plain hand-written mapper - no MapStruct/ModelMapper dependency added for
 * a single small entity with five mapping methods; that would be the kind
 * of unnecessary abstraction the project brief warns against.
 */
@Component
public class DsaProblemMapper {

    /** Builds a new, unsaved entity from a create request, owned by the given user. */
    public DsaProblem toEntity(CreateDsaProblemRequest request, User owner) {
        return DsaProblem.builder()
                .title(request.title())
                .platform(request.platform())
                .problemUrl(request.problemUrl())
                .topic(request.topic())
                .difficulty(request.difficulty())
                .status(request.status())
                .dateSolved(request.dateSolved())
                .timeTaken(request.timeTaken())
                .notes(request.notes())
                .revisionDate(request.revisionDate())
                .user(owner)
                .build();
    }

    /** Applies a full-replace update request onto an already-loaded, owned entity. */
    public void applyUpdate(DsaProblem problem, UpdateDsaProblemRequest request) {
        problem.setTitle(request.title());
        problem.setPlatform(request.platform());
        problem.setProblemUrl(request.problemUrl());
        problem.setTopic(request.topic());
        problem.setDifficulty(request.difficulty());
        problem.setStatus(request.status());
        problem.setDateSolved(request.dateSolved());
        problem.setTimeTaken(request.timeTaken());
        problem.setNotes(request.notes());
        problem.setRevisionDate(request.revisionDate());
    }

    public DsaProblemResponse toResponse(DsaProblem problem) {
        return new DsaProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getPlatform(),
                problem.getProblemUrl(),
                problem.getTopic(),
                problem.getDifficulty(),
                problem.getStatus(),
                problem.getDateSolved(),
                problem.getTimeTaken(),
                problem.getNotes(),
                problem.getRevisionDate(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
