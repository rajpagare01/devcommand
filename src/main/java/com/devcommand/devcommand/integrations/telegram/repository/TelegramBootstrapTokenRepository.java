package com.devcommand.devcommand.integrations.telegram.repository;

import com.devcommand.devcommand.integrations.telegram.entity.TelegramBootstrapToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramBootstrapTokenRepository extends JpaRepository<TelegramBootstrapToken, String> {

    @Modifying
    @Query("DELETE FROM TelegramBootstrapToken t WHERE t.tokenHash = :tokenHash")
    int deleteByTokenHash(String tokenHash);
}
