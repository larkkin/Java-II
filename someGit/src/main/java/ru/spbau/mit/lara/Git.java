package ru.spbau.mit.lara;

import ru.spbau.mit.lara.exceptions.GitException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Git {
    private GitTree start;
    private GitTree head;
    private Path rootDir;

    public Git(Path rootDir) {
        this.rootDir = rootDir;
        start = new GitTree(rootDir);
        head = start;
        int currentCommitId = head.currentCommitId;
        Path gitDir = Paths.get(rootDir.toString(), ".augit");
        List<File> files = Arrays.asList(gitDir.toFile().listFiles());
        files.sort(Comparator.comparing(f -> Integer.valueOf(f
                .toPath()
                .getName(f.toPath().getNameCount() - 1)
                .toString()
                .substring(7))));
        for (File dir : files) {
            Path path = dir.toPath();
            if (path.getName(path.getNameCount() - 1).toString().startsWith("commit_") && dir.isDirectory()) {
                currentCommitId++;
                GitTree currentCommit = new GitTree(rootDir);
                currentCommit.currentCommitId = currentCommitId;
                try (BufferedReader br = Files.newBufferedReader(Paths.get(path.toAbsolutePath().toString(),
                                                            ".augitinfo"))) {
                    currentCommit.time = br.readLine();
                    currentCommit.message = br.readLine();
                } catch (IOException e) {
                    return;
                }
                currentCommit.parent = head;
                head.next = currentCommit;
                head = currentCommit;
            }
        }
    }
//    public Git(String rootDirString) {
//        Path rootDirPath = null;
//        try {
//            rootDirPath = Shell.getRoot(rootDirString);
//        } catch (IOException e) {}
//
//        if (rootDirPath != null) {
//            rootDir = rootDirPath;
//        }
//    }
    public void commit(String message) throws IOException {
        head.next = new GitTree(head);
        head.next.parent = head;
        head.next.currentCommitId = head.currentCommitId + 1;
        head.next.time = LocalDateTime.now().toString();
        head.next.message = message;
        Path dirPath = head.next.getDirPath();
        dirPath.toFile().mkdir();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dirPath.toString(), ".augitinfo"))) {
            writer.write(head.next.time);
            writer.newLine();
            writer.write(head.next.message);
        }
        head = head.next;
    }
    public void undoCommit() {
        GitTree prev = head.parent;
        prev.next = null;
        head.rootDir.toFile().delete();
        head = prev;
    }
    public Path getHeadDirPath() {
        return head.getDirPath();
    }
    public GitTree getRevision(int revisionNumber) {
        if (revisionNumber < 1 || revisionNumber > head.currentCommitId) {
            return null;
        }
        GitTree currentCommit = head;
        while (currentCommit != null && currentCommit.currentCommitId != revisionNumber) {
            currentCommit = currentCommit.parent;
        }
        return currentCommit;
    }
    public List<String> log(int revisionNumber) {
        List<String> res = new ArrayList<>();
        if (revisionNumber < 1) {
            revisionNumber = 1;
        }
        if (revisionNumber > head.currentCommitId) {
            return res;
        }
        GitTree currentCommit = head;
        while (currentCommit != null && currentCommit.currentCommitId >= revisionNumber) {
            res.add(currentCommit.time + ", " + currentCommit.message);
            currentCommit = currentCommit.parent;
        }
        return res;
    }
    public void reset(int revisionNumber) throws GitException {
        if (revisionNumber < 1 || revisionNumber > head.currentCommitId) {
            throw new GitException();
        }
        while (head.currentCommitId != revisionNumber) {
            undoCommit();
        }
    }


    public static class GitTree {
        private Path rootDir;
        private GitTree parent;
        private GitTree next;
        private ArrayList<GitTree> sideChildren;
        private int currentCommitId;
        private String time;
        private String message;

        public GitTree(Path rootDir) {
            this.rootDir = rootDir;
        }
        public GitTree(GitTree other) {
            rootDir = other.rootDir;
        }

        public GitTree getHead() {
            if (next == null) {
                return this;
            } else {
                return next.getHead();
            }
        }

        public Path getDirPath() {
            return Paths.get(rootDir.toString(), ".augit", "commit_" + String.valueOf(currentCommitId));
        }




    }
}
