package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ForEachable;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeqProducerTest {
    public static <T> List<T> collect(SeqProducer<? extends T> seqProducer) throws Exception {
        return collect(Cancellation.UNCANCELABLE_TOKEN, seqProducer);
    }

    public static <T> List<T> collect(
            CancellationToken cancelToken,
            SeqProducer<? extends T> seqProducer) throws Exception {

        List<T> result = new ArrayList<>();
        seqProducer.transferAll(cancelToken, element -> {
            cancelToken.checkCanceled();
            result.add(element);
        });
        return result;
    }

    @Test
    public void testToFluentUnwrapsToSame() {
        SeqProducer<?> producer = (cancelToken, consumer) -> System.out.println("dummy");
        assertSame(producer, producer.toFluent().unwrap());
    }

    @Test
    public void testEmptySingleton() throws Exception {
        assertSame(SeqProducer.empty(), SeqProducer.empty());
    }

    @Test
    public void testEmptyProducer() throws Exception {
        assertEquals(Collections.emptyList(), collect(SeqProducer.empty()));
    }

    @Test
    public void testFlatteningEmpty() throws Exception {
        SeqProducer<String> producer = SeqProducer.<List<String>>empty()
                .toFluent()
                .apply(SeqProducer::flatteningProducer)
                .unwrap();
        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testFlattening() throws Exception {
        List<List<String>> src = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d", "e"),
                Arrays.asList(),
                Arrays.asList("f")
        );

        SeqProducer<String> producer = SeqProducer.iterableProducer(src)
                .toFluent()
                .apply(SeqProducer::flatteningProducer)
                .unwrap();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), collect(producer));
    }

    @Test
    public void testIterableProducer() throws Exception {
        String[] srcValues = {"a", "b", "c"};
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList(srcValues));
        srcValues[1] = "X";
        List<String> result = collect(producer);

        assertEquals(Arrays.asList("a", "X", "c"), result);
    }

    @Test
    public void testForEachableProducer() throws Exception {
        String[] srcValues = {"a", "b", "c"};
        SeqProducer<String> producer = SeqProducer
                .forEachableProducer((ForEachable<String>) Arrays.asList(srcValues)::forEach);
        srcValues[1] = "X";
        List<String> result = collect(producer);

        assertEquals(Arrays.asList("a", "X", "c"), result);
    }

    private void testForEachableProducerFails(Exception producerException) {
        SeqProducer<String> producer = SeqProducer.forEachableProducer(consumer -> {
            consumer.accept("");
        });

        try {
            producer.transferAll(Cancellation.UNCANCELABLE_TOKEN, e -> {
                throw producerException;
            });
            fail("Expected failure.");
        } catch (Exception ex) {
            assertSame(producerException, ex);
        }
    }

    @Test
    public void testForEachableProducerFailsRuntimeException() throws Exception {
        testForEachableProducerFails(new TestRuntimeException());
    }

    @Test
    public void testForEachableProducerFailsException() throws Exception {
        testForEachableProducerFails(new TestException());
    }

    @Test
    public void testArrayProducer() throws Exception {
        String[] srcValues = {"a", "b", "c"};
        SeqProducer<String> producer = SeqProducer.arrayProducer(srcValues);
        srcValues[1] = "X";

        List<String> result = collect(producer);

        assertEquals(Arrays.asList("a", "X", "c"), result);
    }

    @Test
    public void testCopiedArrayProducer() throws Exception {
        String[] expectedValues = {"a", "b", "c"};

        String[] srcValues = expectedValues.clone();
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer(srcValues);
        srcValues[1] = "X";

        List<String> result = collect(producer);

        assertEquals(Arrays.asList(expectedValues.clone()), result);
    }

    private static class TestRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
