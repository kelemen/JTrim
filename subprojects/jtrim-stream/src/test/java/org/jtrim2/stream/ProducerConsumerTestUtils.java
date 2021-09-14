package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ProducerConsumerTestUtils {
    public static void setFirstException(
            AtomicReference<? super RuntimeException> errorRef,
            String message) {

        setFirstException(errorRef, message, null);
    }

    public static void setFirstException(
            AtomicReference<? super RuntimeException> errorRef,
            String message,
            Throwable cause) {

        if (errorRef.get() != null) {
            return;
        }

        errorRef.compareAndSet(null, new RuntimeException(message, cause));
    }

    public static <E extends Throwable> void verifyNoException(AtomicReference<E> errorRef) throws E {
        E error = errorRef.get();
        if (error != null) {
            throw error;
        }
    }

    public static List<String> testStrings(int upToIndex) {
        List<String> result = IntStream.range(0, upToIndex).mapToObj(Integer::toString).collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> repeat(int times, T obj) {
        List<T> result = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            result.add(obj);
        }
        return result;
    }

    private ProducerConsumerTestUtils() {
        throw new AssertionError();
    }
}
