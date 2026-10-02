package com.devcommand.devcommand.integrations.telegram.repository;

import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TelegramPendingConfirmationRepository extends JpaRepository<TelegramPendingConfirmation, String> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TelegramPendingConfirmation c WHERE c.token = :token AND c.user.id = :userId AND c.chatId = :chatId AND c.actionType = :actionType AND c.expiresAt > :now")
    int consumeConfirmation(@Param("token") String token, @Param("userId") Long userId, @Param("chatId") Long chatId, @Param("actionType") String actionType, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TelegramPendingConfirmation c WHERE c.token = :token AND c.user.id = :userId AND c.chatId = :chatId")
    int cancelConfirmation(@Param("token") String token, @Param("userId") Long userId, @Param("chatId") Long chatId);

    @Query("SELECT c FROM TelegramPendingConfirmation c WHERE c.token = :token AND c.user.id = :userId AND c.chatId = :chatId")
    Optional<TelegramPendingConfirmation> findValidConfirmation(@Param("token") String token, @Param("userId") Long userId, @Param("chatId") Long chatId);
}
