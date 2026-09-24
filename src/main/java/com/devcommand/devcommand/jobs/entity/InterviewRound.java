package com.devcommand.devcommand.jobs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single interview round belonging to a JobApplication.
 *
 * Assumption: the prompt did not specify enums for roundType/status here
 * (unlike DsaProblem/JobApplication/Project/DailyTask, which all had
 * explicit enum lists). Rather than invent a business-logic-shaped state
 * machine ahead of time, roundType and status are kept as plain strings for
 * now - this is revisited when interview-round features are implemented.
 * No createdAt/updatedAt was requested for this entity either, so it does
 * not extend BaseEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "interview_rounds")
public class InterviewRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "round_type")
    private String roundType;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterviewRound other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
