package org.jtrim2.jobprocessing;

public interface ElementMapper<T, R> {
    public static <T> ElementMapper<T, T> identity() {
        return ElementMappers.identityMapper();
    }

    public void map(T element, ElementConsumer<? super R> consumer);
}
