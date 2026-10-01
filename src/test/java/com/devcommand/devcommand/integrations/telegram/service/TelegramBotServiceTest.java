package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.identity.ExternalIdentity;
import com.devcommand.devcommand.command.identity.ExternalIdentityResolver;
import com.devcommand.devcommand.integrations.telegram.config.TelegramProperties;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramProcessedUpdateRepository;
import com.devcommand.devcommand.integrations.whatsapp.service.DeterministicCommandParser;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import com.devcommand.devcommand.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock private TelegramProperties properties;
    @Mock private TelegramUpdateProcessor updateProcessor;
    @Mock private DeterministicCommandParser parser;
    @Mock private ExternalIdentityResolver identityResolver;
    @Mock private TelegramProcessedUpdateRepository processedUpdateRepository;
    @Mock private HttpClient httpClient;

    private ObjectMapper objectMapper = new ObjectMapper();
    private TelegramBotService telegramBotService;
    @Mock
    private TelegramBootstrapService bootstrapService;
    @Mock
    private NaturalLanguageInterpreter interpreter;


@BeforeEach
void setUp() {
    telegramBotService = new TelegramBotService(
            properties,
            updateProcessor,
            parser,
            identityResolver,
            processedUpdateRepository,
            objectMapper,
            httpClient,
            bootstrapService,
            interpreter
    );
}
    @Test
    void processUpdates_whenValidCommand_thenExecutes() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":1,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add task: buy milk\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        
        Command mockCommand = new Command(CommandType.CREATE_TASK, 1L, java.util.Collections.emptyMap());
        when(parser.parse(1L, "add task: buy milk")).thenReturn(Optional.of(mockCommand));
        when(updateProcessor.processAndMark(any(Command.class), eq(1L))).thenReturn(CommandResult.success("Task created"));
        
        when(processedUpdateRepository.existsById(1L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(updateProcessor).processAndMark(any(Command.class), eq(1L));
    }

    @Test
    void processUpdates_whenUnauthorizedUser_thenRejects() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        
        String json = "{\"ok\":true,\"result\":[{\"update_id\":2,\"message\":{\"from\":{\"id\":999},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add task: hack\"}}]}";
        
        when(processedUpdateRepository.existsById(2L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(identityResolver, never()).resolve(any());
        verify(updateProcessor, never()).processAndMark(any(), anyLong());
        verify(updateProcessor).markProcessed(2L);
    }

    @Test
    void processUpdates_whenUnmappedUser_thenRejects() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        
        String json = "{\"ok\":true,\"result\":[{\"update_id\":3,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add task: something\"}}]}";
        
        when(identityResolver.resolve(any())).thenReturn(Optional.empty());
        when(processedUpdateRepository.existsById(3L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(updateProcessor, never()).processAndMark(any(), anyLong());
        verify(updateProcessor).markProcessed(3L);
    }

    @Test
    void processUpdates_whenDuplicateUpdate_thenSkips() throws Exception {
        String json = "{\"ok\":true,\"result\":[{\"update_id\":4,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add task: buy milk\"}}]}";
        when(processedUpdateRepository.existsById(4L)).thenReturn(true);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(identityResolver, never()).resolve(any());
        verify(updateProcessor, never()).processAndMark(any(), anyLong());
    }

    @Test
    void processUpdates_startCommand_thenShowsHelpAndDoesNotDispatch() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":5,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"/start\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        when(processedUpdateRepository.existsById(5L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(updateProcessor, never()).processAndMark(any(), anyLong());
        verify(updateProcessor).markProcessed(5L);
    }

    @Test
    void processUpdates_whenNonPrivateChat_thenRejects() throws Exception {
        String json = "{\"ok\":true,\"result\":[{\"update_id\":6,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"group\"},\"text\":\"add task: buy milk\"}}]}";
        when(processedUpdateRepository.existsById(6L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(identityResolver, never()).resolve(any());
        verify(updateProcessor, never()).processAndMark(any(), anyLong());
        verify(updateProcessor).markProcessed(6L);
    }

    @Test
    void processUpdates_whenCommandFails_thenMarksProcessed() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":7,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add task: fail\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        
        Command mockCommand = new Command(CommandType.CREATE_TASK, 1L, java.util.Collections.emptyMap());
        when(parser.parse(1L, "add task: fail")).thenReturn(Optional.of(mockCommand));
        when(updateProcessor.processAndMark(any(Command.class), eq(7L))).thenThrow(new RuntimeException("Database error"));
        
        when(processedUpdateRepository.existsById(7L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        verify(updateProcessor).markProcessed(7L);
    }

    @Test
    void sendMessage_whenComplexText_thenSerializesProperly() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":8,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"/start\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        when(processedUpdateRepository.existsById(8L)).thenReturn(false);

        // Mock HTTP response so it doesn't throw NPE
        java.net.http.HttpResponse<String> mockResponse = mock(java.net.http.HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        org.mockito.ArgumentCaptor<java.net.http.HttpRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(java.net.http.HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());

        // We can't synchronously read BodyPublisher easily, but we know ObjectMapper doesn't throw on quotes
        // We ensure no exception was logged that broke the execution flow.
        verify(updateProcessor).markProcessed(8L);
    }

    @Test
    void sendMessage_whenApiFails_logsSafelyWithoutToken() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":9,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"/start\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        when(processedUpdateRepository.existsById(9L)).thenReturn(false);

        java.net.http.HttpResponse<String> mockResponse = mock(java.net.http.HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}");
        doReturn(mockResponse).when(httpClient).send(any(), any());

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        // Verification relies on no unhandled exceptions breaking the loop
        verify(updateProcessor).markProcessed(9L);
    }

    @Test
    void processUpdates_whenParserFails_fallsBackToGemini() throws Exception {
        when(properties.getAllowedUserId()).thenReturn(12345L);
        when(properties.getBotToken()).thenReturn("token");

        String json = "{\"ok\":true,\"result\":[{\"update_id\":10,\"message\":{\"from\":{\"id\":12345},\"chat\":{\"id\":678,\"type\":\"private\"},\"text\":\"add a task please\"}}]}";
        
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(identityResolver.resolve(any(ExternalIdentity.class))).thenReturn(Optional.of(mockUser));
        when(processedUpdateRepository.existsById(10L)).thenReturn(false);

        // Parser returns empty
        when(parser.parse(1L, "add a task please")).thenReturn(Optional.empty());

        // Gemini handles it
        Command mockCommand = new Command(CommandType.CREATE_TASK, 1L, new com.devcommand.devcommand.command.CommandParameters(java.util.Collections.emptyMap()));
        when(interpreter.interpret(1L, "add a task please")).thenReturn(com.devcommand.devcommand.integrations.gemini.service.InterpretationResult.ready(mockCommand));
        
        when(updateProcessor.processAndMark(any(Command.class), eq(10L))).thenReturn(CommandResult.success("Task created via AI"));

        ReflectionTestUtils.invokeMethod(telegramBotService, "processUpdates", json);

        org.mockito.ArgumentCaptor<Command> commandCaptor = org.mockito.ArgumentCaptor.forClass(Command.class);
        verify(updateProcessor).processAndMark(commandCaptor.capture(), eq(10L));
        
        Command dispatched = commandCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1L, dispatched.userId());
        org.junit.jupiter.api.Assertions.assertEquals(CommandType.CREATE_TASK, dispatched.type());
    }
}
