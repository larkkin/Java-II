package ru.spbau.mit.lara;

import ru.spbau.mit.lara.commands.Init;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Index {

    public static int onlyFirst = 1;
    public static int onlySecond = -1;
    public static int equal = 0;
    public static int changed = 2;
    private Map<Path, List<Path>> dirs;

    public Index() {
        dirs = new HashMap<>();
    }

    private void scanDirectory(Path dirPath, Path rootPath) throws IOException {
        if (rootPath.equals(dirPath)) {
            dirs.put(dirPath, new ArrayList<>());
        }
        List<Path> pathsFromDir = Files.list(dirPath).collect(Collectors.toList());
        for (Path path : pathsFromDir) {
            if (dirPath.relativize(path).toString().startsWith(Init.gitDir)) {
                continue;
            }
            File file = path.toFile();
            if (file.isDirectory()) {
                scanDirectory(path, rootPath);
            } else {
                dirs.get(rootPath).add(rootPath.relativize(path));
            }
        }
    }
    public Map<Path, Integer> compareDirs(Path dir1, Path dir2) throws IOException {
        scanDirectory(dir1, dir1);
        scanDirectory(dir2, dir2);
        Map<Path, Integer> res = new HashMap<>();
        Set<Path> filesFromDir1 =  new HashSet<>(dirs.get(dir1));
        Set<Path> filesFromDir2 =  new HashSet<>(dirs.get(dir2));
        for (Path path1 : filesFromDir1) {
            if (!filesFromDir2.contains(path1)) {
                res.put(path1, onlyFirst);
            } else {
                byte[] file1Content = Files.readAllBytes(Paths.get(dir1.toString(), path1.toString()));
                byte[] file2Content = Files.readAllBytes(Paths.get(dir2.toString(), path1.toString()));
                if (Arrays.equals(file1Content, file2Content)) {
                    res.put(path1, equal);
                } else {
                    res.put(path1, changed);
                }
            }
        }
        for (Path path2 : filesFromDir2) {
            if (!filesFromDir1.contains(path2)) {
                res.put(path2, onlySecond);
            }
        }
        return res;
    }



    private static class FileSystemElement {
        private Path path;
        private boolean isDirectory;
        private boolean valid = false;
        private boolean added = false;
        private List<FileSystemElement> children;
        private Map<Path, FileSystemElement> mapOfChildren;

        public FileSystemElement(Path path, boolean addAll) {
            this.path = path;
            File file = new File(path.toString());
            added = addAll;
            if (file.isDirectory()) {
                isDirectory = true;
                try {
                    mapOfChildren = Files.list(path)
                            .collect(Collectors.toMap(
                                    p -> ((Path) p).getName(((Path) p).getNameCount() - 1),
                                    p -> new FileSystemElement((Path) p, addAll)));
                } catch (IOException e) {
                    valid = false;
                    return;
                }
            }
            valid = true;
        }
    }
}
