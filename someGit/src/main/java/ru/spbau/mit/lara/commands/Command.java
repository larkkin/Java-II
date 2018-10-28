package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.exceptions.ContinueException;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.util.List;

/**
 * It's the interface that allows the commands to
 * implement it in order to unify all the commands
 */
public interface Command {
    /**
     * A method to execute the first command in a pipeline
     * or the single command.
     * @param tokens represents command line arguments/options
     * @return single string (containing newline symbols) that
     * should be printed to the stdout or passed along the pipeline
     */
    String execute(List<String> tokens) throws ExitException;
}

