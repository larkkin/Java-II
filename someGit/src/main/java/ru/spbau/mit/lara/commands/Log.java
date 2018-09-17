package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Log implements Command{
    private Map<Path, Git> gitStorage;

    public Log(Map<Path, Git> gitStorage) {
        this.gitStorage = gitStorage;
    }

    @Override
    public String execute(List<String> tokens) throws ExitException {
        int revision;
        if (tokens.size() < 1) {
            revision = 1;
        } else {
            revision = Integer.valueOf(tokens.get(0));
        }
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
        List<String> commit_descriptions = git.log(revision);
        return String.join("\n", commit_descriptions);
    }
}
