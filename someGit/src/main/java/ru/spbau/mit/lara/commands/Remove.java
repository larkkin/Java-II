package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Remove implements Command {
    private Map<Path, Git> gitStorage;
    private Index index;

    public Remove(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }
    @Override
    public String execute(List<String> tokens) throws ExitException {
        if (tokens.size() < 1) {
            return "Please, specify the files you want to remove";
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
        if (!gitStorage.containsKey(rootDir)) {
            gitStorage.put(rootDir, new Git(rootDir));
        }
        Git git = gitStorage.get(rootDir);
        if (git.getHead().hasNext()) {
            return "HEAD is not on the top of the branch. " +
                    "Please, checkout the current branch to continue (note that modified can be overrided by checkout)";
        }
        if (tokens.size() > 1) {
            return removeFiles(tokens, rootDir, git);
        }
        String branchToRemove = tokens.get(0);
        Set<String> branches;
        try {
            branches = Files.list(Paths.get(rootDir.toString(), Init.gitDir))
                    .filter(p -> p.toFile().isDirectory())
                    .map(p -> Init.getGitDir(rootDir).relativize(p).toString())
                    .filter(s -> s.startsWith("branch_"))
                    .map(s -> s.substring("branch_".length()))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return "Something is wrong with the filesystem, got IOException while indexing branches";
        }
        if (!branches.contains(branchToRemove)) {
            return removeFiles(Collections.singletonList(branchToRemove), rootDir, git);
        }
        return removeBranch(branchToRemove, rootDir, git);
    }

    private String removeBranch(String branchName, Path rootDir, Git git) {
        if (branchName.equals(Git.MASTER)) {
            return "Sorry, you can not remove the main branch";
        }
        if (branchName.equals(git.getCurrentBranch())) {
            return "Sorry, you can not remove current branch";
        }
        try {
            git.removeBranch(branchName);
            Path dir = Paths.get(rootDir.toString(), Init.getBranchDir(branchName).toString());
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while removing branch directory";
        }
        return "Removed branch " + branchName;
    }
    private String removeFiles(List<String> files, Path rootDir, Git git) {
        try {
            Shell.removeFilesRelatively(files, Init.getIndexDir(rootDir));
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while removing";
        }
        return "Removed " + files.size() + " files";
    }
}
