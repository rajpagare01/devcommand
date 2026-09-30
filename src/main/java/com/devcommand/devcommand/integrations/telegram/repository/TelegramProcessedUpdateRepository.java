package com.devcommand.devcommand.integrations.telegram.repository;

import com.devcommand.devcommand.integrations.telegram.entity.TelegramProcessedUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramProcessedUpdateRepository extends JpaRepository<TelegramProcessedUpdate, Long> {
}
