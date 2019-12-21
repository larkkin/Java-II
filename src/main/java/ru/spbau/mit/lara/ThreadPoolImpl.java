package ru.spbau.mit.lara;

import ru.spbau.mit.lara.exceptions.LightExecutionException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;

public class ThreadPoolImpl implements ThreadPool {
    private final Queue<LightFutureImpl<?>> tasks = new LinkedList<>();
    private final Thread[] threads;

    public ThreadPoolImpl(int n) {
        threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            LightFutureImpl<?> task = null;
                            synchronized (tasks) {
                                while (tasks.isEmpty()) {
                                    tasks.wait();
                                }
                                task = tasks.remove();
                            }
                            task.compute();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            threads[i].start();
        }
    }

    @Override
    public <T> LightFuture<T> submit(Supplier<T> supplier) {
        return submit(new LightFutureImpl<T>(this) {
            @Override
            protected T supply() throws LightExecutionException {
                return supplier.get();
            }
        });
    }

    public <T> LightFuture<T> submit(LightFutureImpl<T> task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
        return task;
    }



    @Override
    public void shutdown() {
        for (Thread t : threads) {
            t.interrupt();
        }
    }
}
