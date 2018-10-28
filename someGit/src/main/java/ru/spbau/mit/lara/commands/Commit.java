package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Commit implements Command {
    private Map<Path, Git> gitStorage;

    public Commit(Map<Path, Git> gitStorage) {
        this.gitStorage = gitStorage;
    }
    public String execute(List<String> tokens) throws ExitException {
        if (tokens.size() < 2) {
            return "Please, specify the files you want to commit";
        }
        Path preRootDir = null;
        String message = tokens.get(0);
        List<String> files = tokens.subList(1, tokens.size());
        for (String pathString : files) {
            Path currentRootDir;
            try {
                currentRootDir = Shell.getRoot(pathString);
            } catch (IOException e) {
                return "Something is wrong with one of the files";
            }
            if (currentRootDir == null) {
                return "Something is wrong with one of the files: cannot find VCS root";
            }
            if (preRootDir != null && currentRootDir != preRootDir) {
                return "Error: different VCS roots. All files in a commit must be under the same root";
            }
            preRootDir = currentRootDir;
        }
        final Path rootDir = preRootDir;
        if (!gitStorage.containsKey(rootDir)) {
            gitStorage.put(rootDir, new Git(rootDir));
        }
        Git git = gitStorage.get(rootDir);
        try {
            git.commit(message);
            Path dirPath = git.getHeadDirPath();
            Shell.copyFilesRelatively(files, rootDir, dirPath);
        } catch (IOException e) {
            git.undoCommit();
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        return "Committed " + files.size() + " files";
    }
}
