package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Merge implements Command {
    public static final String CONFLICT_POSTFIX = ".conflict";

    private Map<Path, Git> gitStorage;
    private Index index;

    public Merge(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }

    @Override
    public String execute(List<String> tokens) throws ExitException {
        if (tokens.size() != 1) {
            return "Please, specify the branch you want to merge into the current one";
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

        String currentBranch = git.getCurrentBranch();
        String otherBranch = tokens.get(0);
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
        if (!branches.contains(otherBranch)) {
            return "Error: cannot find branch " + otherBranch;
        }
        if (otherBranch.equals(currentBranch)) {
            return "Error: cannot merge a branch into itself";
        }
        Path otherBranchHeadDir = git.getHead(otherBranch).getDirPath();

        if (git.getBranchParentCommit(otherBranch) == git.getHead()) {
            try {
                Shell.copyFilesRelatively(otherBranchHeadDir, rootDir);
            } catch (IOException e) {
                return "Something is wrong with one of the files: caught IOExcepton while copying";
            }
            return "No conflicts present: please, commit the changes";
        }


        List<Path> onlyCurrent = new ArrayList<>();
        List<Path> conflict = new ArrayList<>();
        List<Path> onlyOther = new ArrayList<>();
        Map<Path, Integer> diff;
        try {
            diff = index.compareDirs(rootDir, otherBranchHeadDir);
        } catch (IOException e) {
            return "Something is wrong with the git directory, got IOException while comparing working and index dirs";
        }
        Status.diffToLists(diff, onlyCurrent, conflict, onlyOther);
        try {
            Shell.copyFilesRelatively(onlyOther
                            .stream()
                            .map(Path::toString)
                            .collect(Collectors.toList()),
                    otherBranchHeadDir,
                    rootDir);
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        try {
            Shell.copyFilesRelativelyWithPostfix(conflict
                            .stream()
                            .map(Path::toString)
                            .collect(Collectors.toList()),
                    otherBranchHeadDir,
                    rootDir,
                    CONFLICT_POSTFIX);
        } catch (IOException e) {
            return "Something is wrong with one of the files: caught IOExcepton while copying";
        }
        if (conflict.isEmpty()) {
            return "No conflicts present: please, commit the changes";
        }
        return "There are some conflicts. Please, resolve them and commit the changes";
    }
}
