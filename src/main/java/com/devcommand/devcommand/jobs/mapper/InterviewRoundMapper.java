package com.devcommand.devcommand.jobs.mapper;

import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.InterviewRoundResponse;
import com.devcommand.devcommand.jobs.dto.UpdateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.entity.InterviewRound;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class InterviewRoundMapper {

    public InterviewRound toEntity(CreateInterviewRoundRequest request, JobApplication parentJob) {
        return InterviewRound.builder()
                .roundNumber(request.roundNumber())
                .roundType(request.roundType())
                .scheduledAt(request.scheduledAt())
                .status(request.status())
                .feedback(request.feedback())
                .notes(request.notes())
                .jobApplication(parentJob)
                .build();
    }

    /** Full-replace update onto an already-loaded, ownership-verified round. Never touches jobApplication. */
    public void applyUpdate(InterviewRound round, UpdateInterviewRoundRequest request) {
        round.setRoundNumber(request.roundNumber());
        round.setRoundType(request.roundType());
        round.setScheduledAt(request.scheduledAt());
        round.setStatus(request.status());
        round.setFeedback(request.feedback());
        round.setNotes(request.notes());
    }

    public InterviewRoundResponse toResponse(InterviewRound round) {
        return new InterviewRoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getRoundType(),
                round.getScheduledAt(),
                round.getStatus(),
                round.getFeedback(),
                round.getNotes()
        );
    }
}
