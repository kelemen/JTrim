package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.executor.SingleThreadedExecutor;
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
                .then(consumer2.toFluent())
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
                .then(SeqConsumer.draining().toFluent())
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
                .then(consumer2.toFluent())
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
                .then(SeqConsumer.<String>draining().toFluent())
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
                .then(consumer2.toFluent())
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

    private <T> void testConsumer(
            List<String> src,
            List<T> expected,
            Function<FluentSeqConsumer<T>, FluentSeqConsumer<String>> applier
    ) throws Exception {
        List<T> collected = new ArrayList<>();
        SeqConsumer<String> consumer = applier
                .apply(SeqConsumerTest.collectingConsumer(collected).toFluent())
                .unwrap();

        SeqProducer.iterableProducer(src)
                .toFluent()
                .withConsumer(consumer)
                .execute(Cancellation.UNCANCELABLE_TOKEN);
        assertEquals(expected, collected);
    }

    private static List<Wrapper> wrapped(String... values) {
        return CollectionsEx.mapToNewList(Arrays.asList(values), Wrapper::new);
    }

    @Test
    public void testMappedSeqIdentity() throws Exception {
        testConsumer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "c"),
                c -> c.mapped(SeqMapper.identity())
        );
    }

    @Test
    public void testMappedSeq() throws Exception {
        testConsumer(
                Arrays.asList("a", "b", "c"),
                wrapped("ax", "bx", "cx"),
                c -> {
                    return c.mapped(SeqMapper
                            .fromElementMapper((e, consumer) -> {
                                consumer.processElement(new Wrapper(e + "x"));
                            })
                    );
                }
        );
    }

    @Test
    public void testMappedSeqFluent() throws Exception {
        testConsumer(
                Arrays.asList("a", "b", "c"),
                wrapped("ax", "bx", "cx"),
                c -> {
                    return c.mapped(SeqMapper
                            .<String, Wrapper>fromElementMapper((e, consumer) -> {
                                consumer.processElement(new Wrapper(e + "x"));
                            })
                            .toFluent()
                    );
                }
        );
    }

    @Test
    public void testMappedContextFree() throws Exception {
        testConsumer(
                Arrays.asList("a", "b", "c"),
                wrapped("ax", "bx", "cx"),
                c -> {
                    return c.mappedContextFree((e, consumer) -> {
                        consumer.processElement(new Wrapper(e + "x"));
                    });
                }
        );
    }

    @Test
    public void testMappedContextFreeIdentity() throws Exception {
        testConsumer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("a", "b", "c"),
                c -> c.mappedContextFree(ElementMapper.identity())
        );
    }

    private void testInBackground(
            Function<FluentSeqConsumer<String>, FluentSeqConsumer<String>> inBackground,
            Consumer<? super String> peekAction
    ) throws Exception {
        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        List<String> result = new ArrayList<>();

        SeqConsumer<String> src = ElementConsumers.contextFreeSeqConsumer(e -> {
            try {
                result.add(e);
                peekAction.accept(e);
            } catch (Throwable ex) {
                setFirstException(testErrorRef, "peek error", ex);
            }
        });

        SeqConsumer<String> consumer = inBackground
                .apply(src.toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("a", "b", "c");
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, SeqProducer.iterableProducer(expected));
        verifyNoException(testErrorRef);
        assertEquals(expected, result);
    }

    @Test(timeout = 10000)
    public void testInBackgroundOwned() throws Exception {
        String executorName = "Test-Executor-testInBackgroundOwned";
        testInBackground(
                consumer -> consumer.inBackground(executorName, 0),
                element -> {
                    String threadName = Thread.currentThread().getName();
                    if (!threadName.contains(executorName)) {
                        throw new IllegalStateException("Expected to run in background, but running in " + threadName);
                    }
                }
        );
    }

    @Test(timeout = 10000)
    public void testInBackgroundExternal() throws Exception {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("Test-Executor-testInBackgroundExternal");
        try {
            testInBackground(
                    consumer -> consumer.inBackground(executor, 0),
                    element -> {
                        if (!executor.isExecutingInThis()) {
                            String threadName = Thread.currentThread().getName();
                            throw new IllegalStateException("Expected to run in background, but running in "
                                    + threadName);
                        }
                    }
            );
        } finally {
            executor.shutdownAndCancel();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }
}
