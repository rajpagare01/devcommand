package com.devcommand.devcommand;

import com.devcommand.devcommand.integrations.telegram.service.TelegramBotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
public class TelegramFlowTest {

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testTelegramAndGeminiFlows() throws Exception {
        long telegramUserId = 1726879886L;
        
        Method handleMessageMethod = TelegramBotService.class.getDeclaredMethod("handleMessage", long.class, com.fasterxml.jackson.databind.JsonNode.class);
        handleMessageMethod.setAccessible(true);

        String[] messages = {
                "/start",
                "/help",
                "add task: Test Telegram integration",
                "I need to finish implementing JWT authentication in DevCommand. Add it to my tasks.",
                "This is garbage text that should not match anything !@#$$#@%"
        };

        long updateId = 1000L;
        for (String text : messages) {
            System.out.println("TESTING: " + text);
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("message_id", updateId);
            
            ObjectNode chatNode = objectMapper.createObjectNode();
            chatNode.put("id", telegramUserId);
            messageNode.set("chat", chatNode);
            
            ObjectNode fromNode = objectMapper.createObjectNode();
            fromNode.put("id", telegramUserId);
            messageNode.set("from", fromNode);
            
            messageNode.put("text", text);

            assertDoesNotThrow(() -> {
                handleMessageMethod.invoke(telegramBotService, updateId, messageNode);
            });
            Thread.sleep(5000); // Allow time for Gemini and async processing
            updateId++;
        }
    }
}

