package ru.spbau.mit;

import org.junit.Test;
import ru.spbau.mit.lara.LightFuture;
import ru.spbau.mit.lara.LightFutureImpl;
import ru.spbau.mit.lara.ThreadPool;
import ru.spbau.mit.lara.ThreadPoolImpl;
import ru.spbau.mit.lara.exceptions.LightExecutionException;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class TestThreadPool {

    @Test(expected = LightExecutionException.class)
    public void testThrow() throws LightExecutionException {
        final int n = 3;
        ThreadPool pool = new ThreadPoolImpl(n);
        LightFuture<Boolean> task = pool.submit(() -> {
            throw new RuntimeException();
        });
        task.get();
        pool.shutdown();
    }

    @Test
    public void testOneTaskNoChildren() throws LightExecutionException {
        final int n = 3;
        ThreadPool pool = new ThreadPoolImpl(n);
        LightFuture<Boolean> task = pool.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignored) {}
            return true;
        });
        assertFalse(task.isReady());
        assertTrue(task.get());
        assertTrue(task.isReady());
        pool.shutdown();
    }

    @Test
    public void testManyTasksNoChildren() throws LightExecutionException {
        final int n = 3;
        final int numTasks = 1000;
        ThreadPool pool = new ThreadPoolImpl(n);
        List<LightFuture<?>> tasks = new ArrayList<>();

        LightFuture<Integer> task = pool.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {}
            return 0;
        });
        tasks.add(task);
        for (int i = 1; i < numTasks; i++) {
            final int res = i;
            task = pool.submit(() -> res);
            tasks.add(task);
        }
        for (int i = 0; i < numTasks; i++) {
            assertEquals(tasks.get(i).get(), i);
            assertTrue(tasks.get(i).isReady());
        }
        pool.shutdown();
    }

    @Test
    public void testOneTaskOneChild() throws LightExecutionException {
        final int n = 3;
        ThreadPool pool = new ThreadPoolImpl(n);
        LightFuture<Boolean> task = pool.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignored) {}
            return true;
        });
        LightFuture<Integer> childTask = task.thenApply(b -> b ? 2 : 1);
        assertFalse(childTask.isReady());
        assertEquals(2, (int) childTask.get());
        assertTrue(childTask.isReady());
        pool.shutdown();
    }

    @Test
    public void testThreadsCount() throws LightExecutionException {
        final int n = 3;
        ThreadPool pool = new ThreadPoolImpl(n);
        List<LightFuture<Long>> tasks = new ArrayList<>();
        final Set<Long> threadIDs = new TreeSet<>();

        for (int i = 0; i < n; i++) {
            LightFuture<Long> task = pool.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ignored) {
                }
                return Thread.currentThread().getId();
            });
            tasks.add(task);
        }
        for (int i = 0; i < n; i++) {
            threadIDs.add(tasks.get(i).get());
        }
        assertEquals(n, threadIDs.size());
    }
}
