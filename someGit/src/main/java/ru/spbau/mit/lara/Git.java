package ru.spbau.mit.lara;

import ru.spbau.mit.lara.commands.Init;
import ru.spbau.mit.lara.exceptions.GitException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Git {
    private GitTree start;
    private GitTree head;
    private Path rootDir;

    public Git(Path rootDir) {
        this.rootDir = rootDir;
        start = new GitTree(rootDir);
        head = start;
        int currentCommitId = head.currentCommitId;
        Path gitDir = Paths.get(rootDir.toString(), Init.gitDir);
        List<File> files = Stream.of(gitDir.toFile().listFiles())
                .map(f -> gitDir.relativize(f.toPath()))
                .filter(p -> p.toString().startsWith("commit"))
                .sorted(Comparator.comparing(p -> Integer.valueOf(p
                        .getName(p.getNameCount() - 1)
                        .toString()
                        .substring(7))))
                .map(Path::toFile)
                .collect(Collectors.toList());
        for (File dir : files) {
            Path path = Paths.get(gitDir.toString(), dir.toString());
            dir = path.toFile();
            if (dir.isDirectory()) {
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
    public void preCommit(String message) throws IOException {
        head.next = new GitTree(head);
        head.next.parent = head;
        head.next.currentCommitId = head.currentCommitId + 1;
        head.next.time = LocalDateTime.now().toString();
        head.next.message = message;
        Path dirPath = head.next.getDirPath();
        System.out.println("==== " + dirPath.toString());
        dirPath.toFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dirPath.toString(), ".augitinfo"))) {
            writer.write(head.next.time);
            writer.newLine();
            writer.write(head.next.message);
        }
        head = head.next;
    }
    public void postCommit(Index index) throws IOException {
        Path gitlistPath = Paths.get(rootDir.toString(), Init.getCommitDir(head.currentCommitId), Init.gitlist);
        try (BufferedWriter writer = Files.newBufferedWriter(gitlistPath)) {
            Set<Path> files = Files.walk(head.getDirPath())
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toSet());
            for (Path p : files) {
                writer.write(head.getDirPath().relativize(p).toString());
                writer.newLine();
            }
            if (head.parent == start) {
                return;
            }
            Map<Path, Integer> diffHeadPrev = index.compareDirs(head.getDirPath(), head.parent.getDirPath());
            for (Map.Entry<Path, Integer> e : diffHeadPrev.entrySet()) {
                if (e.getValue() == Index.equal) {
                    Files.delete(Paths.get(head.parent.getDirPath().toString(), e.getKey().toString()));
                }
            }
        }
        }
    public void undoCommit() {
        GitTree prev = head.parent;
        prev.next = null;
        if (prev != start) {
            String line;
            Set<Path> prevFiles = new HashSet<>();
            Path gitlistPath = Paths.get(rootDir.toString(), Init.getCommitDir(head.parent.currentCommitId), Init.gitlist);
            try (BufferedReader br = Files.newBufferedReader(gitlistPath);) {
                while ((line = br.readLine()) != null) {
                    Path path = new File(line).toPath();
                    prevFiles.add(path);
                }
                Files.walk(prev.getDirPath())
                        .sorted(Comparator.reverseOrder())
                        .map(prev.getDirPath()::relativize)
                        .filter(prevFiles::contains)
                        .forEach(prevFiles::remove);
                Shell.copyFilesRelatively(
                        prevFiles.stream().map(Path::toString).collect(Collectors.toList()),
                        head.getDirPath(),
                        prev.getDirPath());
            } catch (IOException e) {}
        }
        try {
            Files.walk(head.getDirPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {}
        head = prev;
    }
    public Path getHeadDirPath() {
        return head.getDirPath();
    }
    public GitTree getRevision(int revisionNumber) throws GitException {
        if (revisionNumber < 1 || revisionNumber > head.currentCommitId) {
            throw new GitException();
        }
        GitTree currentCommit = head;
        while (currentCommit != null && currentCommit.currentCommitId != revisionNumber) {
            currentCommit = currentCommit.parent;
        }
        return currentCommit;
    }
    public void copyFilesFromCommit(GitTree revision, Path dir) throws IOException {
        int currentCommitId = revision.currentCommitId;

        Set<Path> filesLeft= new HashSet<>();
        Path gitlistPath = Paths.get(revision.rootDir.toString(), Init.getCommitDir(currentCommitId), Init.gitlist);
        try (BufferedReader br = Files.newBufferedReader(gitlistPath);) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("in list:\t " + line);
                Path path = new File(line).toPath();
                if (path.toString().startsWith(".") || path.toString().isEmpty()) {
                    continue;
                }
                filesLeft.add(path);
            }
        } catch (IOException e) {}
        filesLeft.stream().forEach(p -> System.out.println(p.toString()));
        while (!filesLeft.isEmpty() && revision != null) {
            Set<Path> filesInDir = Files.walk(revision.getDirPath())
                    .map(revision.getDirPath()::relativize)
                    .collect(Collectors.toSet());
            for (Path p : filesInDir) {
                if (filesLeft.contains(p)) {
                    Path oldPath = Paths.get(revision.getDirPath().toString(), p.toString());
                    Path newPath = Paths.get(dir.toString(), p.toString());
                    System.out.println("old " + oldPath.toString());
                    System.out.println("new " + newPath.toString());
                    Files.copy(oldPath, newPath, REPLACE_EXISTING);
                    filesLeft.remove(p);
                }
            }
            revision = revision.next;
        }
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

    public int getCurrentCommitId() {
        return head.currentCommitId;
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
            return Paths.get(rootDir.toString(), Init.getCommitDir(currentCommitId));
        }




    }
}
