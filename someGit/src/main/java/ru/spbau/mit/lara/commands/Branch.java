package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Branch implements Command {
    private Map<Path, Git> gitStorage;
    private Index index;

    public Branch(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }

    @Override
    public String execute(List<String> tokens) {
        if (tokens.size() < 1) {
            return "Please specify the branch name";
        }
        String branchName = tokens.get(0);
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
        try {
            git.makeBranch(branchName);;
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        return "Created new branch \"" + branchName + "\"";
    }
}
