package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;

public class FluentSeqConsumerTest {
    @Test
    public void testApply() {
        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            System.out.println("testApply.a");
        };
        SeqConsumer<String> consumer2 = (cancelToken, seqProducer) -> {
            System.out.println("testApply.b");
        };

        SeqConsumer<String> combined = consumer1
                .toFluent()
                .apply(prev -> {
                    if (prev == consumer1) {
                        return consumer2;
                    } else {
                        throw new AssertionError("Unexpected previous consumer: " + prev);
                    }
                })
                .unwrap();

        assertSame(consumer2, combined);
    }

    @Test
    public void testThen() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> consumer2 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "y"));
        };

        SeqConsumer<String> combined = consumer1
                .toFluent()
                .then(consumer2)
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "ay", "bx", "by", "cx", "cy");
        assertEquals(expected, results);
    }

    @Test
    public void testThenNoOp() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> combined = consumer1
                .toFluent()
                .then(SeqConsumer.draining())
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "bx", "cx");
        assertEquals(expected, results);
    }

    @Test
    public void testThenPreNoOp() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer2 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> combined = SeqConsumer.<String>draining()
                .toFluent()
                .then(consumer2)
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "bx", "cx");
        assertEquals(expected, results);
    }

    @Test
    public void testThenAllNoOp() {
        SeqConsumer<String> combined = SeqConsumer.<String>draining()
                .toFluent()
                .then(SeqConsumer.<String>draining())
                .unwrap();

        assertSame(SeqConsumer.draining(), combined);
    }

    @Test
    public void testThenCancelDetected() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        AtomicReference<RuntimeException> verificationFailureRef = new AtomicReference<>();

        List<String> results = new ArrayList<>();
        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> consumer2 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> {
                if ("b".equals(e)) {
                    cancellation.getController().cancel();
                    if (!cancelToken.isCanceled()) {
                        setFirstException(verificationFailureRef, "cancellation expected.");
                    }
                    throw new OperationCanceledException();
                }
                results.add(e + "y");
            });
        };

        SeqConsumer<String> combined = consumer1
                .toFluent()
                .then(consumer2)
                .unwrap();

        assertEquals(Collections.emptyList(), results);

        try {
            combined.consumeAll(cancellation.getToken(), SeqProducer.copiedArrayProducer("a", "b", "c"));
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // expected
        }

        verifyNoException(verificationFailureRef);

        List<String> expected = Arrays.asList("ax", "ay", "bx");
        assertEquals(expected, results);
    }

    @Test
    public void testThenContextFree() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> combined = consumer1
                .toFluent()
                .thenContextFree(e -> results.add(e + "y"))
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "ay", "bx", "by", "cx", "cy");
        assertEquals(expected, results);
    }

    @Test
    public void testThenContextFreeNoOp() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer1 = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };
        SeqConsumer<String> combined = consumer1
                .toFluent()
                .thenContextFree(ElementConsumer.noOp())
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "bx", "cx");
        assertEquals(expected, results);
    }

    @Test
    public void testThenContextFreePreNoOp() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> combined = SeqConsumer.<String>draining()
                .toFluent()
                .thenContextFree(e -> results.add(e + "x"))
                .unwrap();

        assertEquals(Collections.emptyList(), results);
        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.copiedArrayProducer("a", "b", "c"));

        List<String> expected = Arrays.asList("ax", "bx", "cx");
        assertEquals(expected, results);
    }

    @Test
    public void testThenContextFreeAllNoOp() {
        SeqConsumer<String> combined = SeqConsumer.<String>draining()
                .toFluent()
                .thenContextFree(ElementConsumer.noOp())
                .unwrap();

        assertSame(SeqConsumer.draining(), combined);
    }

    @Test
    public void testAsContextFreeGroupConsumer() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };

        SeqGroupConsumer<String> groupConsumer = consumer.toFluent()
                .asContextFreeGroupConsumer()
                .unwrap();

        groupConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, FluentSeqGroupProducerTest.iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList()
        ));

        List<String> expected = Arrays.asList("ax", "bx", "cx", "dx", "ex");
        assertEquals(expected, results);
    }

    @Test
    public void testAsContextFreeGroupConsumerNoOp() {
        SeqGroupConsumer<?> groupConsumer = SeqConsumer.draining()
                .toFluent()
                .asContextFreeGroupConsumer()
                .unwrap();
        assertSame(SeqGroupConsumer.draining(), groupConsumer);
    }

    @Test
    public void testAsSingleShotGroupConsumer() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };

        SeqGroupConsumer<String> groupConsumer = consumer.toFluent()
                .asSingleShotGroupConsumer()
                .unwrap();

        groupConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, FluentSeqGroupProducerTest.iterableProducer(
                Arrays.asList("a", "b", "c", "d")
        ));

        List<String> expected = Arrays.asList("ax", "bx", "cx", "dx");
        assertEquals(expected, results);
    }

    @Test
    public void testAsSingleShotGroupConsumerMultiGroup() throws Exception {
        List<String> results = new ArrayList<>();

        SeqConsumer<String> consumer = (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, e -> results.add(e + "x"));
        };

        SeqGroupConsumer<String> groupConsumer = consumer.toFluent()
                .asSingleShotGroupConsumer()
                .unwrap();

        try {
            groupConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, FluentSeqGroupProducerTest.iterableProducer(
                    Arrays.asList("a", "b", "c"),
                    Arrays.asList("d", "e"),
                    Arrays.asList()
            ));
            fail("Expected failure.");
        } catch (IllegalStateException ex) {
            // expected
        }

        List<String> expected = Arrays.asList("ax", "bx", "cx");
        assertEquals(expected, results);
    }
}
