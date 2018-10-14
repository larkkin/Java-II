package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.exceptions.ContinueException;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * It's the interface that allows the commands to
 * implement it in order to unify all the commands
 */
public interface Command {

    String execute(List<String> tokens) throws ExitException;

    default boolean noConflicts(Path rootDir) throws IOException {
        return Files.walk(rootDir)
                .noneMatch(p -> p.toString().endsWith(Merge.CONFLICT_POSTFIX));
    }
}

