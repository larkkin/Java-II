package ru.spbau.mit.lara;

import ru.spbau.mit.lara.exceptions.LightExecutionException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;

public interface LightFuture<T> {

    T get() throws LightExecutionException;

    boolean isReady();

    <R> LightFuture<R> thenApply(Function<? super T, R> f);
}


