package ru.spbau.mit.lara;

import org.junit.*;
import ru.spbau.mit.lara.exceptions.ContinueException;
import ru.spbau.mit.lara.exceptions.ExitException;
import ru.spbau.mit.lara.exceptions.GitException;
import ru.spbau.mit.lara.exceptions.ShellException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class GlobalTest {

    private static void run(String args) throws GitException, ContinueException, ShellException,
            ExitException {
        Shell k = new Shell();
        k.processLine(args);
    }
    private static void runMultipleCommands(List<String> commands) throws ContinueException, ShellException,
            ExitException {
        Shell k = new Shell();
        for (String cmd : commands) {
            k.processLine(cmd);
        }
    }

    private static String cat(String path) throws IOException  {
        byte[] content = Files.readAllBytes(Paths.get(path));
        return new String(content, Charset.defaultCharset());
    }

    @Before
    public void setup() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        run("init");
        new ProcessBuilder("bash", "-c", "mkdir inner_dir").start().waitFor();
        new ProcessBuilder("bash", "-c",
                "touch inner_dir/1.txt inner_dir/2.txt inner_dir/3.txt").start().waitFor();
    }

    @After
    public void clear() throws IOException, InterruptedException {
        new ProcessBuilder("bash", "-c", "rm -r inner_dir .augit").start().waitFor();
    }

    @Test
    public void addAndRemove() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        run("add inner_dir/1.txt inner_dir/2.txt");
        run("commit 1");

        run("remove inner_dir/2.txt");
        new ProcessBuilder("bash", "-c", "echo rm_test > inner_dir/2.txt").start().waitFor();
        run("commit 2");
        assertEquals("rm_test\n", cat("inner_dir/2.txt"));

        new ProcessBuilder("bash", "-c", "echo gotanygrapes?_msg > inner_dir/2.txt").start().waitFor();
        run("add inner_dir/3.txt");
        run("commit 3");
        run("checkout 1");
        assertEquals("gotanygrapes?_msg\n", cat("inner_dir/2.txt"));
    }

    @Test
    public void checkoutRevisionBackward() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        new ProcessBuilder("bash", "-c", "echo heypampampam > inner_dir/1.txt").start().waitFor();
        run("commit 1");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 2");
        new ProcessBuilder("bash", "-c", "echo gotanygrapes? > inner_dir/1.txt").start().waitFor();
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 3");
        run("checkout 2");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
    }
    @Test
    public void checkoutRevisionForward() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        new ProcessBuilder("bash", "-c", "echo heypampampam > inner_dir/1.txt").start().waitFor();
        run("commit 1");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 2");
        new ProcessBuilder("bash", "-c", "echo gotanygrapes? > inner_dir/1.txt").start().waitFor();
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 3");
        run("checkout 2");
        run("checkout master");
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
    }

    @Test
    public void checkoutFile() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        new ProcessBuilder("bash", "-c", "echo heypampampam > inner_dir/1.txt").start().waitFor();
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");

        new ProcessBuilder("bash", "-c", "echo gotanygrapes? > inner_dir/1.txt").start().waitFor();
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
        run("checkout inner_dir/1.txt");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
    }

    @Test
    public void makeAndCheckoutBranchAndMerge() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        new ProcessBuilder("bash", "-c", "echo heypampampam > inner_dir/1.txt").start().waitFor();
        run("commit 1");
        run("add inner_dir/1.txt");
        run("commit 2");
        run("branch testing");
        assertEquals("heypampampam\n", cat(".augit/branch_testing/commit_1/inner_dir/1.txt"));
        new ProcessBuilder("bash", "-c", "echo gotanygrapes? > inner_dir/1.txt").start().waitFor();
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 3");
        runMultipleCommands(Arrays.asList("checkout testing", "merge master"));
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt.conflict"));
        }


    @Test
    public void reset() throws GitException, IOException, InterruptedException, ContinueException, ShellException, ExitException {
        new ProcessBuilder("bash", "-c", "echo heypampampam > inner_dir/1.txt").start().waitFor();
        run("commit 1");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 2");
        new ProcessBuilder("bash", "-c", "echo gotanygrapes? > inner_dir/1.txt").start().waitFor();
        assertEquals("gotanygrapes?\n", cat("inner_dir/1.txt"));
        run("add inner_dir/1.txt");
        run("commit 3");
        run("reset 2");
        run("checkout master");
        assertEquals("heypampampam\n", cat("inner_dir/1.txt"));
    }



}

