package com.devcommand.devcommand.user.entity;

import com.devcommand.devcommand.common.entity.BaseEntity;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core user account. Owns every other piece of personal data in the system.
 *
 * Lombok note: @Data is intentionally avoided here. @Data generates
 * equals/hashCode/toString over every field, including the *ToMany
 * collections below - that drags the whole object graph into memory,
 * risks StackOverflowError on bidirectional relationships, and produces
 * unstable equals/hashCode for entities managed by JPA/Hibernate.
 * Getter/Setter + a manual id-based equals/hashCode is the safe pattern.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt hash only. Never returned from any API - DTOs never carry this
     * field, and it is excluded from equals/hashCode/toString below.
     */
    @Column(nullable = false)
    private String password;

    // ---- Relationships -------------------------------------------------
    // mappedBy = owned by the "many" side (FK lives on the child table).
    // JsonIgnore prevents accidental serialization/recursion if a User is
    // ever returned directly; feature controllers will use DTOs instead.
    // Lazy fetch by default - collections are loaded only when explicitly
    // queried via the child repositories, not eagerly with the user.

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DsaProblem> dsaProblems = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> jobApplications = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LearningTopic> learningTopics = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyTask> dailyTasks = new ArrayList<>();

    // ---- equals/hashCode -------------------------------------------------
    // Identity-based equals/hashCode using only the database id, and only
    // when the id has actually been assigned. This avoids the classic JPA
    // trap of using generated/mutable fields (or all fields, via @Data) in
    // equals/hashCode, which breaks Set/Map semantics as an entity moves
    // from transient -> persisted, and avoids infinite recursion through
    // bidirectional relationships.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{id=%d, name='%s', email='%s'}".formatted(id, name, email);
    }
}
