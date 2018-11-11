package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Index;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Status implements Command {
    private Map<Path, Git> gitStorage;
    private Index index;

    public Status(Map<Path, Git> gitStorage, Index index) {
        this.gitStorage = gitStorage;
        this.index = index;
    }

    @Override
    public String execute(List<String> tokens) throws ExitException {
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

        List<Path> createdNonAddedFiles = new ArrayList<>();
        List<Path> changedNonAddedFiles = new ArrayList<>();
        List<Path> removedNonAddedFiles = new ArrayList<>();
        Map<Path, Integer> diffWorkingIndex;
        Path indexDir = Init.getIndexDir(rootDir);
        try {
            diffWorkingIndex = index.compareDirs(rootDir, indexDir);
        } catch (IOException e) {
            return "Something is wrong with the git directory, got IOException while comparing working and index dirs";
        }
        diffToLists(diffWorkingIndex, createdNonAddedFiles, changedNonAddedFiles, removedNonAddedFiles);

        List<Path> createdAddedFiles = new ArrayList<>();
        List<Path> changedAddedFiles = new ArrayList<>();
        List<Path> removedAddedFiles = new ArrayList<>();
        Map<Path, Integer> diffIndexHead;
        try {
            if (git.getCurrentCommitId() > 0) {
                diffIndexHead = index.compareDirs(indexDir, git.getHeadDirPath());
            } else {
                diffIndexHead = Files.walk(indexDir)
                        .map(indexDir::relativize)
                        .collect(Collectors.toMap(p -> p,
                                                  p -> Index.onlyFirst));
            }
        } catch (IOException e) {
            return "Something is wrong with the git directory, got IOException while comparing working and index dirs";
        }
        diffToLists(diffIndexHead, createdAddedFiles, changedAddedFiles, removedAddedFiles);

        List<String> res_list = new ArrayList<>();
        res_list.add("On branch " + git.getHead().getBranch() + ":\n\n");
        res_list.add("==================");
        res_list.add("New added files:");
        res_list.add("------------------");
        for (Path path : createdAddedFiles) {
            res_list.add(path.toString());
        }
        res_list.add("==================");
        res_list.add("Added changes:");
        res_list.add("------------------");
        for (Path path : changedAddedFiles) {
            res_list.add(path.toString());
        }
        res_list.add("==================");
        res_list.add("(added removes) Files removed from index:");
        res_list.add("------------------");
        for (Path path : removedAddedFiles) {
            res_list.add(path.toString());
        }
        res_list.add("==================");
        res_list.add("Untracked files:");
        res_list.add("------------------");
        for (Path path : createdNonAddedFiles) {
            res_list.add(path.toString());
        }
        res_list.add("==================");
        res_list.add("Modified files:");
        res_list.add("------------------");
        for (Path path : changedNonAddedFiles) {
            res_list.add(path.toString());
        }
        res_list.add("==================");
        res_list.add("(not added removes) Files removed from index:");
        res_list.add("------------------");
        for (Path path : removedNonAddedFiles) {
            res_list.add(path.toString());
        }
        return String.join("\n", res_list);
    }
    public static void diffToLists(Map<Path, Integer> diff,
                             List<Path> createdFiles, 
                             List<Path> changedFiles, 
                             List<Path> removedFiles) {
        for (Map.Entry<Path, Integer> pair : diff.entrySet()) {
            if (pair.getValue() == Index.onlyFirst) {
                createdFiles.add(pair.getKey());
            }
            if (pair.getValue() == Index.changed) {
                changedFiles.add(pair.getKey());
            }
            if (pair.getValue() == Index.onlySecond) {
                removedFiles.add(pair.getKey());
            }
        }
    }
}
