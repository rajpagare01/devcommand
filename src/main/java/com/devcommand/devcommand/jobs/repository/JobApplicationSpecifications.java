package com.devcommand.devcommand.jobs.repository;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small, explicit Specification builders - matching the pattern already
 * established by DsaProblemSpecifications/DailyTaskSpecifications.
 * JobApplicationService always includes ownerIs(userId) first in every
 * combination it builds, so filtering/search can never return another
 * user's applications.
 *
 * company/role/source filters are case-insensitive EXACT matches (mirrors
 * the DSA module's topic/platform filters); "search" is the separate,
 * partial-match OR-across-fields query the spec calls out distinctly.
 */
public final class JobApplicationSpecifications {

    private JobApplicationSpecifications() {
    }

    public static Specification<JobApplication> ownerIs(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<JobApplication> companyEquals(String company) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("company")), company.toUpperCase());
    }

    public static Specification<JobApplication> roleEquals(String role) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("role")), role.toUpperCase());
    }

    public static Specification<JobApplication> sourceEquals(String source) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("source")), source.toUpperCase());
    }

    public static Specification<JobApplication> statusEquals(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** company OR role contains the keyword, case-insensitive. Not an advanced search engine on purpose. */
    public static Specification<JobApplication> searchKeyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("company")), pattern),
                cb.like(cb.lower(root.get("role")), pattern)
        );
    }
}
