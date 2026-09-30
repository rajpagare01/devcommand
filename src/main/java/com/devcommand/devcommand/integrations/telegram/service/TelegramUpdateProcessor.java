package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramProcessedUpdate;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramProcessedUpdateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramUpdateProcessor {

    private final CommandDispatcher dispatcher;
    private final TelegramProcessedUpdateRepository repository;

    public TelegramUpdateProcessor(CommandDispatcher dispatcher, TelegramProcessedUpdateRepository repository) {
        this.dispatcher = dispatcher;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommandResult processAndMark(Command command, long updateId) {
        CommandResult result = dispatcher.dispatch(command);
        repository.save(new TelegramProcessedUpdate(updateId));
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(long updateId) {
        if (!repository.existsById(updateId)) {
            repository.save(new TelegramProcessedUpdate(updateId));
        }
    }
}
