package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeqConsumerTest {
    public static <T> SeqConsumer<T> collectingConsumer(List<? super T> result) {
        return (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, result::add);
        };
    }

    @Test
    public void testFlatteningConsumerNoOp() throws Exception {
        SeqConsumer<Collection<String>> consumer
                = SeqConsumer.flatteningConsumer(SeqConsumer.draining());

        assertSame(SeqConsumer.draining(), consumer);
    }

    @Test
    public void testFlatteningConsumer() throws Exception {
        SeqProducer<List<String>> src = SeqProducer.copiedArrayProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e"),
                Arrays.asList()
        );

        List<String> result = new ArrayList<>();
        SeqConsumer<String> baseConsumer = collectingConsumer(result);

        SeqConsumer<Collection<String>> consumer
                = SeqConsumer.flatteningConsumer(baseConsumer);

        assertEquals(Collections.emptyList(), result);

        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, src);

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, result);
    }

    private void testFlatteningConsumerFailsException(Exception consumerException) {
        SeqProducer<List<String>> src = SeqProducer.copiedArrayProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );

        SeqConsumer<String> baseConsumer = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> {
                throw consumerException;
            });
        };

        SeqConsumer<Collection<String>> consumer
                = SeqConsumer.flatteningConsumer(baseConsumer);

        try {
            consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, src);
            fail("Expected failure");
        } catch (Exception ex) {
            assertSame(consumerException, ex);
        }
    }

    @Test
    public void testFlatteningConsumerFailsRuntimeException() throws Exception {
        testFlatteningConsumerFailsException(new TestRuntimeException());
    }

    @Test
    public void testFlatteningConsumerFailsException() throws Exception {
        testFlatteningConsumerFailsException(new TestException());
    }

    @Test
    public void testDraining() throws Exception {
        SeqConsumer<String> seqConsumer = SeqConsumer.draining();

        AtomicInteger pulledRef = new AtomicInteger(0);
        seqConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, (cancelToken, consumer) -> {
            consumer.processElement("a");
            consumer.processElement("b");
            pulledRef.incrementAndGet();
        });
        assertEquals(1, pulledRef.get());
    }

    @Test
    public void testDrainingCancelToken() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        SeqConsumer<String> seqConsumer = SeqConsumer.draining();

        AtomicInteger canceledRef = new AtomicInteger(0);
        seqConsumer.consumeAll(cancellation.getToken(), (cancelToken, consumer) -> {
            cancellation.getController().cancel();
            if (cancelToken.isCanceled()) {
                canceledRef.incrementAndGet();
            }
        });
        assertEquals(1, canceledRef.get());
    }

    private static class TestRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
