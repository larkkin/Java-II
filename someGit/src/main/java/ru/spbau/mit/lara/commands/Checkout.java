package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Checkout implements Command {

    private Map<Path, Git> gitStorage;

    public Checkout(Map<Path, Git> gitStorage) {
        this.gitStorage = gitStorage;
    }

    @Override
    public String execute(List<String> tokens) throws ExitException {
        if (tokens.size() < 1) {
            return "Please, specify the revision";
        }
        int revision = Integer.valueOf(tokens.get(0));
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
        Git git = gitStorage.get(rootDir);
        Git.GitTree currentCommit = git.getRevision(revision);
        Path commitDir = currentCommit.getDirPath();
        try {
            Shell.copyFilesRelatively(commitDir, rootDir);
        } catch (IOException e) {
            return "Error: caught IOException while copying files to the working directory";
        }
        return "Checked out revision " + String.valueOf(revision);
    }
}
