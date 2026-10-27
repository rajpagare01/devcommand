package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ReadDsaProblemCommandHandler implements CommandHandler {

    private final DsaProblemService dsaProblemService;
    private final DsaProblemRepository dsaProblemRepository;
    private final DsaProblemMapper mapper;

    public ReadDsaProblemCommandHandler(DsaProblemService dsaProblemService, DsaProblemRepository dsaProblemRepository, DsaProblemMapper mapper) {
        this.dsaProblemService = dsaProblemService;
        this.dsaProblemRepository = dsaProblemRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.READ_DSA_PROBLEM;
    }

    @Override
    public CommandResult handle(Command command) {
        CommandParameters params = command.parameters();
        Long userId = command.userId();

        Optional<String> idOpt = params.optionalString("id");
        if (idOpt.isPresent()) {
            try {
                Long id = Long.parseLong(idOpt.get());
                DsaProblemResponse resp = dsaProblemService.getById(id, userId);
                return CommandResult.success("Found DSA problem '" + resp.title() + "': Difficulty is " + resp.difficulty() + " (Topic: " + resp.topic() + ").", resp);
            } catch (Exception e) {
                return CommandResult.failure("Problem not found with id: " + idOpt.get());
            }
        }

        Optional<String> titleOpt = params.optionalString("title");
        if (titleOpt.isPresent() && !titleOpt.get().isBlank()) {
            Optional<DsaProblem> problemOpt = dsaProblemRepository.findFirstByUserIdAndTitleIgnoreCase(userId, titleOpt.get());
            if (problemOpt.isPresent()) {
                DsaProblemResponse resp = mapper.toResponse(problemOpt.get());
                return CommandResult.success("Found DSA problem '" + resp.title() + "': Difficulty is " + resp.difficulty() + " (Topic: " + resp.topic() + ").", resp);
            }
            return CommandResult.failure("DSA problem not found with title: " + titleOpt.get());
        }

        return CommandResult.failure("Either problem ID or title must be provided.");
    }
}
