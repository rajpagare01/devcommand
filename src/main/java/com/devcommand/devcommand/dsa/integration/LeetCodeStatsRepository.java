package com.devcommand.devcommand.dsa.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LeetCodeStatsRepository extends JpaRepository<LeetCodeStats, Long> {
    Optional<LeetCodeStats> findByExternalDsaAccountId(Long accountId);
}
