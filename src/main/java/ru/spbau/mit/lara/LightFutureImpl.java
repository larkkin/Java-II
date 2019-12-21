package ru.spbau.mit.lara;

import ru.spbau.mit.lara.exceptions.LightExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class LightFutureImpl<T> implements LightFuture<T> {
    private final ThreadPoolImpl pool;
    private boolean ready = false;
    private boolean failed = false;
    private T result = null;
    private final List<LightFutureImpl<?>> childTasks = new ArrayList<>();

    public LightFutureImpl(ThreadPoolImpl pool) {
        this.pool = pool;
    }

    protected abstract T supply() throws LightExecutionException;

    @Override
    public T get() throws LightExecutionException {
        if (!isReady()) {
            synchronized (this) {
                while (!isReady()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();;
                    }
                }
            }
        }
        if (failed) {
            throw new LightExecutionException();
        }
        return result;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public <R> LightFuture<R> thenApply(Function<? super T, R> f) {
        LightFutureImpl<T> parent = this;
        LightFutureImpl<R> task = new LightFutureImpl<R>(pool) {
            @Override
            protected R supply() throws LightExecutionException {
                return f.apply(parent.get());
            }
        };
        if (isReady()) {
            pool.submit(task);
        } else {
            synchronized (this) {
                childTasks.add(task);
            }
        }
        return task;
    }

    public void compute() {
        try {
            result = supply();
        } catch (Exception e) {
            failed = true;
        }
        ready = true;
        if (!failed) {
            for (LightFutureImpl<?> task : childTasks) {
                pool.submit(task);
            }
        }
        synchronized (this) {
            notifyAll();
        }
    }
}
