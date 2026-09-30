package com.devcommand.devcommand.integrations.identity.repository;

import com.devcommand.devcommand.integrations.identity.entity.ExternalIdentityVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExternalIdentityVerificationChallengeRepository extends JpaRepository<ExternalIdentityVerificationChallenge, Long> {

    @Query("SELECT c FROM ExternalIdentityVerificationChallenge c " +
           "WHERE c.externalIdentity.id = :identityId " +
           "AND c.consumedAt IS NULL " +
           "ORDER BY c.createdAt DESC " +
           "LIMIT 1")
    Optional<ExternalIdentityVerificationChallenge> findLatestActiveChallenge(@Param("identityId") Long identityId);
}
