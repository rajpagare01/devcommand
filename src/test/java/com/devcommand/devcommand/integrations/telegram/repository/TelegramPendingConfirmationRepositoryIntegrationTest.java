package com.devcommand.devcommand.integrations.telegram.repository;

import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.devcommand.devcommand.integration.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TelegramPendingConfirmationRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TelegramPendingConfirmationRepository confirmationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testAtomicConsumption() throws InterruptedException {
        // Prepare test data
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("test_hash");
        User savedUser = userRepository.saveAndFlush(user);

        String token = UUID.randomUUID().toString();
        TelegramPendingConfirmation conf = new TelegramPendingConfirmation(
                token, savedUser, 12345L, "DELETE_TASK", 999L, LocalDateTime.now().plusMinutes(5)
        );
        confirmationRepository.saveAndFlush(conf);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalConsumed = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    // Try to consume atomically
                    int consumed = confirmationRepository.consumeConfirmation(
                            token, savedUser.getId(), 12345L, "DELETE_TASK", LocalDateTime.now()
                    );
                    totalConsumed.addAndGet(consumed);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly ONE thread should have successfully consumed the row
        assertEquals(1, totalConsumed.get(), "Only one atomic consumption should succeed.");
        assertTrue(confirmationRepository.findById(token).isEmpty());
    }

    @Test
    void testCancelConfirmation() {
        User newUser = new User();
        newUser.setName("Cancel");
        newUser.setEmail("cancel@example.com");
        newUser.setPassword("hash");
        User user = userRepository.saveAndFlush(newUser);
        String token = UUID.randomUUID().toString();
        confirmationRepository.saveAndFlush(new TelegramPendingConfirmation(
                token, user, 12345L, "DELETE_TASK", 999L, LocalDateTime.now().plusMinutes(5)
        ));

        int cancelled = confirmationRepository.cancelConfirmation(token, user.getId(), 12345L);
        assertEquals(1, cancelled);
        
        Optional<TelegramPendingConfirmation> after = confirmationRepository.findById(token);
        assertTrue(after.isEmpty());
    }
}
