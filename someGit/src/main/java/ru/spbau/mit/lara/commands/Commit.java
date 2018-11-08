package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Commit implements Command {
    private Map<Path, Git> gitStorage;
    private Index index;

    public Commit(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }

    public String execute(List<String> tokens) throws ExitException {
        String message = String.join(" ", tokens);
        Path rootDir;
        try {
            rootDir = Shell.getRootOfDir(System.getProperty("user.dir"));
        } catch (IOException e) {
            return "Something is wrong with the filesystem, got IOException while looking for the VCS root";
        }
        if (rootDir == null) {
            return "Error: current directory is not under augit";
        }
        if (!gitStorage.containsKey(rootDir)) {
            gitStorage.put(rootDir, new Git(rootDir));
        }
        try {
            if (!noConflicts(rootDir)) {
                return "There are some conflicts. Please, resolve them and commit your changes";
            }
        } catch (IOException e) {
            return "Something is wrong with the filesystem, got IOException while scanning the working dir";
        }
        Git git = gitStorage.get(rootDir);
        if (git.getHead().hasNext()) {
            return "HEAD is not on the top of the branch. " +
                    "Please, checkout the current branch to continue (note that modified can be overrided by checkout)";
        }
        try {
            git.preCommit(message);
            Path dirPath = git.getHeadDirPath();
            Shell.copyFilesRelatively(Init.getIndexDir(rootDir), dirPath);
            git.postCommit(index);
        } catch (IOException e) {
            git.undoCommit();
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        return "Committed new revision";
    }
}
