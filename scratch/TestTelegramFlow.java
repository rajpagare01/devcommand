import org.springframework.context.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.devcommand.devcommand.integrations.telegram.service.TelegramBotService;
import com.devcommand.devcommand.DevcommandApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class TestTelegramFlow {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting TestTelegramFlow...");
        
        // Start the Spring Boot context
        ConfigurableApplicationContext context = SpringApplication.run(DevcommandApplication.class, args);
        
        try {
            TelegramBotService telegramBotService = context.getBean(TelegramBotService.class);
            long telegramUserId = 1726879886L; // The allowed user ID

            System.out.println("Running /start test...");
            telegramBotService.handleMessage(telegramUserId, "/start");
            Thread.sleep(1000);

            System.out.println("Running /help test...");
            telegramBotService.handleMessage(telegramUserId, "/help");
            Thread.sleep(1000);

            System.out.println("Running deterministic add task test...");
            telegramBotService.handleMessage(telegramUserId, "add task: Test Telegram integration");
            Thread.sleep(2000);

            System.out.println("Running Gemini natural language test...");
            telegramBotService.handleMessage(telegramUserId, "I need to finish implementing JWT authentication in DevCommand. Add it to my tasks.");
            Thread.sleep(5000); // Wait for Gemini API response

            System.out.println("Testing Gemini failure safety...");
            // Test how it handles an unexpected failure or an invalid command that Gemini can't parse
            telegramBotService.handleMessage(telegramUserId, "Blah blah do something you can't possibly understand #@!@#");
            Thread.sleep(5000);

            System.out.println("All flows executed. Check database to verify task creation.");
        } finally {
            context.close();
        }
    }
}
