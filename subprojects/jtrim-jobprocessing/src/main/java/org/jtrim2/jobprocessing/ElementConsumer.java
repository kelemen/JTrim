package org.jtrim2.jobprocessing;

public interface ElementConsumer<T> {
    public static <T> ElementConsumer<T> noOp() {
        return ElementConsumers.noOpConsumer();
    }

    public void processElement(T element) throws Exception;
}
