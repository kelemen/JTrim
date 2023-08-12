
package org.jtrim2.collections;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IterableStreamableTest {
    @SuppressWarnings("unchecked")
    private static <T> Class<Stream<T>> streamClass() {
        return (Class<Stream<T>>) (Class<?>) Stream.class;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<Collection<T>> collectionClass() {
        return (Class<Collection<T>>) (Class<?>) Collection.class;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<Consumer<T>> consumerClass() {
        return (Class<Consumer<T>>) (Class<?>) Consumer.class;
    }

    @Test
    public void testStream() {
        Collection<String> collection = mock(collectionClass());
        Stream<String> stream = mock(streamClass());

        when(collection.stream()).thenReturn(stream);

        Streamable<String> streamable = Streamable.fromCollection(collection);

        assertSame(stream, streamable.stream());

        verify(collection).stream();
        verifyNoMoreInteractions(stream, collection);
    }

    @Test
    public void testForEach() {
        Collection<String> collection = mock(collectionClass());
        Consumer<String> consumer = mock(consumerClass());

        Streamable<String> streamable = Streamable.fromCollection(collection);

        streamable.forEach(consumer);

        verify(collection).forEach(same(consumer));

        verifyNoMoreInteractions(consumer, collection);
    }
}
