package com.devcommand.devcommand.dsa.integration;

import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalDsaAccountRepository extends JpaRepository<ExternalDsaAccount, Long> {
    Optional<ExternalDsaAccount> findByUserIdAndPlatform(Long userId, DsaPlatform platform);
    boolean existsByUserIdAndPlatform(Long userId, DsaPlatform platform);
}
