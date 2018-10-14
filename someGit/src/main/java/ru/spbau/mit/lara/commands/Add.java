package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Add implements Command {
    private Map<Path, Git> gitStorage;
    private Index index;

    public Add(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }


    @Override
    public String execute(List<String> tokens) throws ExitException {
        if (tokens.size() < 1) {
            return "Please, specify the files you want to add";
        }
        Path preRootDir = null;
        for (String pathString : tokens) {
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
                return "Error: different VCS roots. All files must be under the same root";
            }
            preRootDir = currentRootDir;
        }
        final Path rootDir = preRootDir;
        try {
            if (!noConflicts(rootDir)) {
                return "There are some conflicts. Please, resolve them and commit your changes";
            }
        } catch (IOException e) {
            return "Something is wrong with the filesystem, got IOException while scanning the working dir";
        }
        if (!gitStorage.containsKey(rootDir)) {
            gitStorage.put(rootDir, new Git(rootDir));
        }
        Git git = gitStorage.get(rootDir);
        try {
            Shell.copyFilesRelatively(tokens, rootDir, Init.getIndexDir(rootDir));
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        return "Added " + tokens.size() + " files";
    }
}
