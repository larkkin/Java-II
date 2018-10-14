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
    private Map<String, GitTree> headStorage;
    private Path rootDir;
    private static final String MASTER = "master";

    public Git(Path rootDir) {
        this.rootDir = rootDir;
        start = new GitTree(rootDir);
        head = start;
        head.currentBranch = MASTER;
        head.parent = start;
        headStorage = new HashMap<>();
        Path gitDir = Paths.get(rootDir.toString(), Init.gitDir);
        headStorage.put(head.currentBranch, head);
        List<String> branches = Stream.of(gitDir.toFile().listFiles())
                .map(f -> gitDir.relativize(f.toPath()))
                .map(Path::toString)
                .filter(s -> s.startsWith("branch_"))
                .map(s -> s.substring("branch_".length()))
                .collect(Collectors.toList());
        List<ParentEntry> parentsToAdd = new ArrayList<>();
        for (String currentBranch : branches) {
            Path branchDir = Paths.get(gitDir.toString(), "branch_" + currentBranch);
            List<File> commits = Stream.of(branchDir.toFile().listFiles())
                    .map(f -> branchDir.relativize(f.toPath()))
                    .filter(p -> p.toString().startsWith("commit"))
                    .sorted(Comparator.comparing(p -> Integer.valueOf(p
                            .getName(p.getNameCount() - 1)
                            .toString()
                            .substring("commit_".length()))))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            int currentCommitId = 0;
            for (File dir : commits) {
                Path path = Paths.get(branchDir.toString(), dir.toString());
                dir = path.toFile();
                if (dir.isDirectory()) {
                    currentCommitId++;
                    GitTree currentCommit = new GitTree(rootDir);
                    currentCommit.currentCommitId = currentCommitId;
                    currentCommit.currentBranch = currentBranch;
                    int parentCommitId;
                    String parentBranch;
                    try (BufferedReader br = Files.newBufferedReader(Paths.get(path.toAbsolutePath().toString(),
                            ".augitinfo"))) {
                        currentCommit.time = br.readLine();
                        currentCommit.message = br.readLine();
                        parentCommitId = Integer.valueOf(br.readLine());
                        parentBranch = br.readLine();
                    } catch (IOException e) {
                        return;
                    }
                    if (parentBranch.equals(currentBranch)) {
                        GitTree branchHead = headStorage.get(currentBranch);
                        currentCommit.parent = branchHead;
                        branchHead.next = currentCommit;
                    } else {
                        parentsToAdd.add(new ParentEntry(currentCommitId, currentBranch, parentCommitId, parentBranch));
                    }
                    headStorage.put(currentBranch, currentCommit);
                }
            }
        }
        for (ParentEntry p : parentsToAdd) {
            GitTree commit = getNode(p.branch, p.id);
            GitTree parent = getNode(p.parentBranch, p.parentId);
            commit.parent = parent;
            parent.sideChildren.add(commit);
        }
        head = headStorage.get(MASTER);
    }

    private GitTree getNode(String branch, int id) {
        if (!headStorage.containsKey(branch)) {
            return null;
        }
        GitTree currentCommit = headStorage.get(branch);
        if (id > currentCommit.currentCommitId || id < 0) {
            return null;
        }
        while (currentCommit.currentCommitId != id && currentCommit.parent != null) {
            currentCommit = currentCommit.parent;
        }
        return currentCommit;
    }

    private static class ParentEntry {
        int id;
        String branch;
        int parentId;
        String parentBranch;

        public ParentEntry(int id, String branch, int parentId, String parentBranch) {
            this.id = id;
            this.branch = branch;
            this.parentId = parentId;
            this.parentBranch = parentBranch;
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
        GitTree new_node = new GitTree(head);
        head.next = new_node;
        new_node.parent = head;
        new_node.currentCommitId = head.currentCommitId + 1;
        new_node.currentBranch = head.currentBranch;
        new_node.time = LocalDateTime.now().toString();
        new_node.message = message;
        Path dirPath = new_node.getDirPath();
        System.out.println("==== " + dirPath.toString());
        dirPath.toFile().mkdirs();
        writeGitInfo(new_node);
        head = new_node;
        headStorage.put(head.currentBranch, head);
    }

    private void writeGitInfo(GitTree new_node) throws IOException {
        Path dirPath = new_node.getDirPath();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dirPath.toString(), ".augitinfo"))) {
            writer.write(new_node.time);
            writer.newLine();
            writer.write(new_node.message);
            writer.newLine();
            writer.write(String.valueOf(new_node.parent.currentCommitId));
            writer.newLine();
            writer.write(new_node.parent.currentBranch);
        }
    }

    public void postCommit(Index index) throws IOException {
        Path gitlistPath = Paths.get(rootDir.toString(),
                                     Init.getCommitDir(head.currentCommitId, head.currentBranch),
                                     Init.gitlist);
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
            Path gitlistPath = Paths.get(rootDir.toString(), Init.getCommitDir(head.parent.currentCommitId, head.parent.currentBranch), Init.gitlist);
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

    public void makeBranch(String branchName) throws IOException {
        GitTree new_node = new GitTree(head);
        head.sideChildren.add(new_node);
        new_node.parent = head;
        new_node.currentCommitId = 1;
        new_node.currentBranch = branchName;
        new_node.time = LocalDateTime.now().toString();
        new_node.message = "Created branch " + branchName;
        headStorage.put(new_node.currentBranch, new_node);
        Path dirPath = new_node.getDirPath();
        System.out.println("==== " + dirPath.toString());
        dirPath.toFile().mkdirs();
        writeGitInfo(new_node);
        copyFilesFromCommit(head, dirPath);
        Path gitlistPath = Paths.get(rootDir.toString(),
                Init.getCommitDir(new_node.currentCommitId, new_node.currentBranch),
                Init.gitlist);
        try (BufferedWriter writer = Files.newBufferedWriter(gitlistPath)) {
            Set<Path> files = Files.walk(new_node.getDirPath())
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toSet());
            for (Path p : files) {
                writer.write(head.getDirPath().relativize(p).toString());
                writer.newLine();
            }
        }
    }

    public Path getHeadDirPath() {
        return head.getDirPath();
    }
    public GitTree getHead() {
        return head;
    }
    public GitTree getHead(String branchName) {
        if (headStorage.containsKey(branchName)) {
            return headStorage.get(branchName);
        } else {
            return null;
        }
    }
    public void setHead(GitTree newHead) {
        head = newHead;
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
        String currentBranch = revision.currentBranch;

        Set<Path> filesLeft= new HashSet<>();
        Path gitlistPath = Paths.get(revision.rootDir.toString(), Init.getCommitDir(currentCommitId, currentBranch), Init.gitlist);
        try (BufferedReader br = Files.newBufferedReader(gitlistPath);) {
            String line;
            while ((line = br.readLine()) != null) {
//                System.out.println("in list:\t " + line);
                Path path = new File(line).toPath();
                if (path.toString().startsWith(".") || path.toString().isEmpty()) {
                    continue;
                }
                filesLeft.add(path);
            }
        } catch (IOException e) {}
//        filesLeft.forEach(p -> System.out.println(p.toString()));
        while (!filesLeft.isEmpty() && revision != null) {
            Set<Path> filesInDir = Files.walk(revision.getDirPath())
                    .filter(p -> p.toFile().isFile())
                    .map(revision.getDirPath()::relativize)
                    .collect(Collectors.toSet());
            for (Path p : filesInDir) {
                if (filesLeft.contains(p)) {
                    Path oldPath = Paths.get(revision.getDirPath().toString(), p.toString());
                    Path newPath = Paths.get(dir.toString(), p.toString());
                    System.out.println("old " + oldPath.toString());
                    System.out.println("new " + newPath.toString());
                    Path parentDir = newPath.getParent();
                    parentDir.toFile().mkdirs();
                    Files.copy(oldPath, newPath, REPLACE_EXISTING);
                    filesLeft.remove(p);
                }
            }
            revision = revision.next;
        }
    }
    public List<String> log(int revisionNumber) {
        List<String> res = new ArrayList<>();
        if (revisionNumber <= 0) {
            revisionNumber = 1;
        }
        if (revisionNumber > head.currentCommitId) {
            return res;
        }
        GitTree currentCommit = head;
        while (currentCommit != null && currentCommit.currentCommitId >= revisionNumber) {
            res.add(currentCommit.time + ",\n" + currentCommit.message + "\non branch: " + currentCommit.currentBranch);
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

    public String getCurrentBranch() {
        return head.currentBranch;
    }



    public static class GitTree {
        private Path rootDir;
        private GitTree parent;
        private GitTree next;
        private ArrayList<GitTree> sideChildren = new ArrayList<>();
        private int currentCommitId;
        private String currentBranch;
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

        public int getCurrentCommitId() {
            return currentCommitId;
        }
        public String getMessage() {
            return message;
        }
        public String getBranch() {
            return currentBranch;
        }

        public Path getDirPath() {
            return Paths.get(rootDir.toString(), Init.getCommitDir(currentCommitId, currentBranch));
        }




    }
}
