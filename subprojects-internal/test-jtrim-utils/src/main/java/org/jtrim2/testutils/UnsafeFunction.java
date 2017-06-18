package org.jtrim2.testutils;

public interface UnsafeFunction<T, R> {
    public R apply(T arg) throws Exception;
}
