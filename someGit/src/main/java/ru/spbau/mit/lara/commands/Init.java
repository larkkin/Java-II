package ru.spbau.mit.lara.commands;

import ru.spbau.mit.lara.exceptions.ExitException;
import ru.spbau.mit.lara.exceptions.InitException;
import ru.spbau.mit.lara.Shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.System;

public class Init implements Command {
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

    public boolean initDir(String path) throws InitException {
        File dir = new File(path + "/.augit");
        boolean created = dir.mkdirs();
        if (!dir.isDirectory()) {
            throw new InitException();
        }
        return created;
    }
    // To versions of echo command. Can be used to echo the values of several variables simultaneously
}

