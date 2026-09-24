package com.devcommand.devcommand.jobs.mapper;

import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.dto.UpdateJobApplicationRequest;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.user.entity.User;
import org.springframework.stereotype.Component;

/** Plain hand-written mapper, matching the pattern established by DsaProblemMapper/DailyTaskMapper. */
@Component
public class JobApplicationMapper {

    public JobApplication toEntity(CreateJobApplicationRequest request, User owner) {
        return JobApplication.builder()
                .company(request.company())
                .role(request.role())
                .location(request.location())
                .jobUrl(request.jobUrl())
                .source(request.source())
                .salary(request.salary())
                .applicationDate(request.applicationDate())
                .status(request.status())
                .notes(request.notes())
                .user(owner)
                .build();
    }

    /** Full-replace update onto an already-loaded, owned entity. Never touches user. */
    public void applyUpdate(JobApplication job, UpdateJobApplicationRequest request) {
        job.setCompany(request.company());
        job.setRole(request.role());
        job.setLocation(request.location());
        job.setJobUrl(request.jobUrl());
        job.setSource(request.source());
        job.setSalary(request.salary());
        job.setApplicationDate(request.applicationDate());
        job.setStatus(request.status());
        job.setNotes(request.notes());
    }

    public JobApplicationResponse toResponse(JobApplication job) {
        return new JobApplicationResponse(
                job.getId(),
                job.getCompany(),
                job.getRole(),
                job.getLocation(),
                job.getJobUrl(),
                job.getSource(),
                job.getSalary(),
                job.getApplicationDate(),
                job.getStatus(),
                job.getNotes(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
