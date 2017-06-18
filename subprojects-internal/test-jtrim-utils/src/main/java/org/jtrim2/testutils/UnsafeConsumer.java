package org.jtrim2.testutils;

public interface UnsafeConsumer<T> {
    public void accept(T arg) throws Exception;
}
