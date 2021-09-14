package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.jtrim2.stream.FluentSeqGroupProducerTest.*;
import static org.junit.Assert.*;

public final class SeqGroupProducerTest {
    public static <T> List<List<T>> collect(SeqGroupProducer<? extends T> seqGroupProducer) throws Exception {
        return collect(Cancellation.UNCANCELABLE_TOKEN, seqGroupProducer);
    }

    public static <T> List<List<T>> collect(
            CancellationToken cancelToken,
            SeqGroupProducer<? extends T> seqGroupProducer) throws Exception {

        List<List<T>> result = Collections.synchronizedList(new ArrayList<>());
        seqGroupProducer.transferAll(cancelToken, (consumerCanelToken, seqProducer) -> {
            List<T> groupResult = SeqProducerTest.collect(consumerCanelToken, seqProducer);
            result.add(groupResult);
        });
        return new ArrayList<>(result);
    }

    @Test
    public void testFlatteningEmpty() throws Exception {
        SeqGroupProducer<String> producer = SeqGroupProducer.<List<String>>empty()
                .toFluent()
                .apply(SeqGroupProducer::flatteningProducer)
                .unwrap();
        assertSame(SeqGroupProducer.empty(), producer);
    }

    @Test
    public void testFlattening() throws Exception {
        List<List<String>> src = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d", "e"),
                Arrays.asList(),
                Arrays.asList("f")
        );

        SeqGroupProducer<String> producer = iterableProducer(src)
                .toFluent()
                .apply(SeqGroupProducer::flatteningProducer)
                .unwrap();
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c", "d", "e", "f")), collect(producer));
    }
}
