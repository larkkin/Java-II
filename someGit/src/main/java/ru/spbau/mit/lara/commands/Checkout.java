package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.Git;
import ru.spbau.mit.lara.Shell;
import ru.spbau.mit.lara.exceptions.ExitException;
import ru.spbau.mit.lara.exceptions.GitException;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

        if (tokens.size() > 1) {
            return checkoutFiles(tokens, rootDir, git);
        }

        int revision = -1;
        String branchToCheckout;
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
        try {
            revision = Integer.valueOf(tokens.get(0));
            return checkoutRevision(revision, rootDir, git);
        } catch (NumberFormatException e) {
            branchToCheckout = tokens.get(0);
            if (branches.contains(branchToCheckout)) {
                return checkoutBranch(branchToCheckout,rootDir, git);
            }
            return checkoutFiles(Collections.singletonList(branchToCheckout), rootDir, git);
        }
    }

    private String checkoutFiles(List<String> files, Path rootDir, Git git) {
        try {
            files = files.stream()
                    .map(s -> rootDir.relativize(new File(s).toPath().toAbsolutePath()).toString())
                    .collect(Collectors.toList()); ;
            Shell.copyFilesRelatively(files, Init.getIndexDir(rootDir), rootDir);
        } catch (IOException e) {
            return "Error: caught IOException while copying files to the working directory";
        }
        return "checked out " + files.size() + " files";
    }
    private String checkoutBranch(String branchName, Path rootDir, Git git) {
        Git.GitTree branchHead = git.getHead(branchName);
        if (branchHead == null) {
            return "No such branch, wtf";
        }
        git.setHead(branchHead);
        try {
            Shell.copyFilesRelatively(branchHead.getDirPath(), rootDir);
        }  catch (IOException e) {
            return "Error: caught IOException while copying files to the working directory";
        }
        return "checked out branch " + branchName;
    }
    private String checkoutRevision(int revision, Path rootDir, Git git) {
        Git.GitTree currentCommit;
        try {
            currentCommit = git.getRevision(revision);
            try {
                git.copyFilesFromCommit(currentCommit, rootDir);
            } catch (IOException e) {
                return "Error: got IOException while checking out commit " + String.valueOf(revision);
            }
        } catch(GitException e) {
            return "Error: invalid revision number";
        }
        Path commitDir = currentCommit.getDirPath();
        try {
            Shell.copyFilesRelatively(commitDir, rootDir);
        } catch (IOException e) {
            return "Error: caught IOException while copying files to the working directory";
        }
        return "Checked out revision " + String.valueOf(revision);
    }
}
