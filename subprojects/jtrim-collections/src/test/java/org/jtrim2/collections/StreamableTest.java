package org.jtrim2.collections;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StreamableTest {
    @SuppressWarnings("unchecked")
    private static <T> Class<Stream<T>> streamClass() {
        return (Class<Stream<T>>) (Class<?>) Stream.class;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<Consumer<T>> consumerClass() {
        return (Class<Consumer<T>>) (Class<?>) Consumer.class;
    }

    @Test
    public void testForEach() {
        Stream<String> stream = mock(streamClass());
        Consumer<String> consumer = mock(consumerClass());
        AtomicInteger counter = new AtomicInteger(0);

        Streamable<String> streamable = () -> {
            counter.incrementAndGet();
            return stream;
        };

        streamable.forEach(consumer);

        assertEquals("stream() call count", 1, counter.get());

        verify(stream).forEach(same(consumer));
    }
}
