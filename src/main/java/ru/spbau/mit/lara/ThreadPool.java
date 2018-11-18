package ru.spbau.mit.lara;

import java.util.function.Supplier;

public interface ThreadPool {

    <T> LightFuture<T> submit(Supplier<T> supplier);
    void shutdown();

}