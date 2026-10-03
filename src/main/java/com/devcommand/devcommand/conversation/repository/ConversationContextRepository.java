package com.devcommand.devcommand.conversation.repository;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface ConversationContextRepository extends JpaRepository<ConversationContext, Long> {
    Optional<ConversationContext> findByUserIdAndChatId(Long userId, String chatId);
    
    @Modifying
    void deleteByUserIdAndChatId(Long userId, String chatId);

    @Modifying
    @Query("DELETE FROM ConversationContext c WHERE c.expiresAt < :now")
    int deleteExpired(ZonedDateTime now);
}
