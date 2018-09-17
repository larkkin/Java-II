package ru.spbau.mit.lara;

import ru.spbau.mit.lara.commands.*;
import ru.spbau.mit.lara.exceptions.ContinueException;
import ru.spbau.mit.lara.exceptions.ExitException;
import ru.spbau.mit.lara.exceptions.ShellException;
//import ru.spbau.mit.lara.exceptions.ShellRuntimeException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * It is the main class that receives the input line
 * from stdin, proceeds it and executes the command
 */
public class Shell {
    private HashMap<String, Command> commandStorage;
    private HashMap<Path, Git> gitStorage;

    /**
     * Initiating the fields
     */
    Shell() {
        commandStorage = new HashMap<>();
        gitStorage = new HashMap<>();
        /*
         * We put the commands avaliable by now in the commandStorage: the list can be enlarged.
         */
        commandStorage.put("commit", new Commit(gitStorage));
        commandStorage.put("checkout", new Checkout(gitStorage));
        commandStorage.put("log", new Log(gitStorage));
        commandStorage.put("reset", new Reset(gitStorage));
        commandStorage.put("init", new Init());
//        //commandStorage.put("cat", new Cat());
//        commandStorage.put("exit", new Exit());
//        commandStorage.put("pwd", new Pwd());
//        commandStorage.put("wc", new Wc());
//        commandStorage.put("grep", new Grep());
    }


    /**
     * This function is written to avoid the duplicated code
     */
    private static String[] getArrayFromStringAndList(String str, List<String> lst) {
        String[] arr = new String[1 + lst.size()];
        arr[0] = str;
        for (int i = 0; i < lst.size(); i++) {
            arr[i + 1] = lst.get(i);
        }
        return arr;
    }

    /**
     * If we are given the command without the pipe and simply execute it
     */
    private void executeCommand(String commandName, List<String> arguments) throws ExitException {
        if (commandStorage.containsKey(commandName)) {
            System.out.println(commandStorage.get(commandName).execute(arguments));
        }
        else {
                System.out.println(
                        "no such command: " + commandName);
        }
    }

    /**
     * The main shell method. we arrive here when the input line is written in Main class
     */
    public void processLine(String inputLine) throws ShellException, ExitException, ContinueException {
        StringTokenizer tokenizer = new StringTokenizer(inputLine, " ");
        if (!tokenizer.hasMoreTokens()) {
            throw new ContinueException();
        }
        String commandName = tokenizer.nextToken();
        List<String> tokensList = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            tokensList.add(tokenizer.nextToken());
        }
        executeCommand(commandName, tokensList);
    }

    public static Path getRootOfDir(String pathString) throws IOException {
        Path path = new File(pathString).toPath().toAbsolutePath();
        while (path != null) {
            List<String> filesInDir = Files.list(path)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            if (filesInDir.contains(".augit")) {
//                System.out.println("Find VCS root at " + path.toString());
                return path;
            }
            path = path.getParent();
        }
        return null;
    }
    public static Path getRoot(String pathString) throws IOException {
        return getRootOfDir(new File(pathString).toPath().toAbsolutePath().getParent().toString());
    }

    public static void copyFilesRelatively(List<String> files, Path srcDir, Path dstDir) throws IOException {
        List<Path> relativePaths = files.stream()
                .map(p ->  new File(p).toPath().toAbsolutePath())
                .filter(p -> !p.getName(p.getNameCount() - 1).toString().startsWith("."))
                .peek(p -> System.out.println("->   " + p))
                .map(p -> srcDir.relativize(p))
                .peek(p -> System.out.println("->   " + p))
                .collect(Collectors.toList());
        for (Path p : relativePaths) {
            System.out.println("!!->   " + p.toString());
            Path oldPath = Paths.get(srcDir.toString(), p.toString());
            Path newPath = Paths.get(dstDir.toString(), p.toString());
            System.out.println("src file: " + oldPath.toString());
            System.out.println("dst file: " + newPath.toString());
            System.out.println(".");
            Files.copy(oldPath, newPath);
        };
    }
    public static void copyFilesRelatively(Path srcDir, Path dstDir) throws IOException {
        List<String> files = Arrays.stream(srcDir.toFile().listFiles())
                .map(File::toString)
                .collect(Collectors.toList());
        copyFilesRelatively(files, srcDir, dstDir);
    }


}
