package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.exceptions.ExitException;
import ru.spbau.mit.lara.exceptions.InitException;
import ru.spbau.mit.lara.Shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.lang.System;

public class Init implements Command {
    public static final String gitDir = ".augit";
    private static final String indexDir = Paths.get(".augit", ".gitindex").toString();
    public static final String gitlist = ".augitlist";
    public static final String branch = "branch_";

    public static String getCommitDir(int i, String branchName) {
        return Paths.get(gitDir, branch + branchName, "commit_" + String.valueOf(i)).toString();
    }
    public static Path getIndexDir(Path rootDir) {
        return Paths.get(rootDir.toString(), indexDir);
    }
    public static Path getGitDir(Path rootDir) {
        return Paths.get(rootDir.toString(), gitDir);
    }

    public String execute(List<String> tokens) throws ExitException {
        String destinationDir;
        if (tokens.size() < 1) {
            destinationDir = System.getProperty("user.dir");
        } else {
            destinationDir = tokens.get(0);
        }
        Path rootPath;
        try {
            rootPath = Shell.getRoot(destinationDir);
        } catch (IOException e){
            throw new ExitException();
        }
        if (rootPath != null) {
            return "Directory " + destinationDir + " is already under augit";
        }
        boolean created;
        try {
            created = initDir(destinationDir);
        } catch (InitException e) {
            return "Error: unable to create the directory";
        }
        if (created) {
            return "Initialized augit repository at " + destinationDir;
        } else {
            return "Directory " + destinationDir + " is already under augit";
        }
    }

    private boolean initDir(String path) throws InitException {
        File dir = new File(Paths.get(path, gitDir).toString());
        boolean created = dir.mkdirs();
        if (!dir.isDirectory()) {
            throw new InitException();
        }
        System.out.println(Paths.get(path, indexDir).toString());
        File dirIndex = new File(Paths.get(path, indexDir).toString());
        boolean created_index = dirIndex.mkdirs();
        if (!dirIndex.isDirectory()) {
            throw new InitException();
        }
        return created;
    }
}

