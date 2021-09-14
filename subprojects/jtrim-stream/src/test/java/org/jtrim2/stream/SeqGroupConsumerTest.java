package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeqGroupConsumerTest {
    public static <T> SeqGroupConsumer<T> collectingConsumer(List<List<T>> result) {
        return collectingConsumer(result, Function.identity());
    }

    public static <T> SeqGroupConsumer<T> collectingConsumer(
            List<List<T>> result,
            Function<? super T, ? extends T> elementMapper) {

        return (cancelToken, seqGroupProducer) -> {
            seqGroupProducer.transferAll(cancelToken, collectingBasicConsumer(result, elementMapper));
        };
    }

    public static <T> SeqConsumer<T> collectingBasicConsumer(
            List<List<T>> result,
            Function<? super T, ? extends T> elementMapper) {

        return (consumerCancelToken, seqProducer) -> {
            List<T> groupResult = new ArrayList<>();
            result.add(groupResult);
            seqProducer.transferAll(consumerCancelToken, e -> groupResult.add(elementMapper.apply(e)));
        };
    }

    @Test
    public void testFlatteningConsumerNoOp() throws Exception {
        SeqGroupConsumer<Collection<String>> consumer
                = SeqGroupConsumer.flatteningConsumer(SeqGroupConsumer.draining());

        assertSame(SeqGroupConsumer.draining(), consumer);
    }

    @Test
    public void testFlatteningConsumer() throws Exception {
        SeqGroupProducer<List<String>> src = FluentSeqGroupProducerTest.<List<String>>iterableProducer(
                Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e")),
                Arrays.asList(Arrays.asList("f", "g"), Arrays.asList("h")),
                Arrays.asList()
        );

        List<List<String>> result = Collections.synchronizedList(new ArrayList<>());
        SeqGroupConsumer<String> baseConsumer = collectingConsumer(result);

        SeqGroupConsumer<Collection<String>> consumer
                = SeqGroupConsumer.flatteningConsumer(baseConsumer);

        assertEquals(Collections.emptyList(), result);

        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, src);

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c", "d", "e"),
                Arrays.asList("f", "g", "h"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testDraining() throws Exception {
        SeqGroupConsumer<String> seqGroupConsumer = SeqGroupConsumer.draining();

        AtomicInteger pulledRef = new AtomicInteger(0);
        seqGroupConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, (cancelToken, seqConsumer) -> {
            seqConsumer.consumeAll(cancelToken, (cancelToken2, consumer) -> {
                consumer.processElement("a");
                consumer.processElement("b");
                pulledRef.incrementAndGet();
            });
        });
        assertEquals(1, pulledRef.get());
    }

    @Test
    public void testDrainingCancelToken() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        SeqGroupConsumer<String> seqGroupConsumer = SeqGroupConsumer.draining();

        AtomicInteger canceledRef = new AtomicInteger(0);
        seqGroupConsumer.consumeAll(cancellation.getToken(), (cancelToken, seqConsumer) -> {
            seqConsumer.consumeAll(cancelToken, (cancelToken2, consumer) -> {
                cancellation.getController().cancel();
                if (cancelToken2.isCanceled()) {
                    canceledRef.incrementAndGet();
                }
            });
        });
        assertEquals(1, canceledRef.get());
    }
}
