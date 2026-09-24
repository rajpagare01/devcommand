package com.devcommand.devcommand.jobs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity correction made in this stage: `status` was originally a plain
 * String (see the now-outdated comment this replaces) because no enum had
 * been specified yet and the field was flagged to be "revisited when
 * interview-round features are implemented." That's now - `status` is a
 * proper InterviewStatus enum (SCHEDULED/COMPLETED/CANCELLED/RESCHEDULED),
 * matching how every other status-like field in the project is modeled
 * (@Enumerated(EnumType.STRING), matching the DB column to a fixed set of
 * values instead of an arbitrary string). `roundType` remains a String -
 * no enum was specified for it (it's free text like "Technical" or "HR"),
 * so it's left as-is rather than inventing a closed set of round types.
 * `ddl-auto=update` will widen/alter the existing varchar column
 * automatically in dev; this project has no production data yet, so no
 * migration script was needed.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

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
