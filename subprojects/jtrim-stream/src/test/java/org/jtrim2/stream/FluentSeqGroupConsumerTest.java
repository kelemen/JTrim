package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.jtrim2.stream.SeqGroupConsumerTest.*;
import static org.junit.Assert.*;

public class FluentSeqGroupConsumerTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(ElementConsumers.class);
    }

    @Test
    public void testApply() {
        testApply(FluentSeqGroupConsumer::apply);
    }

    @Test
    public void testApplyFluent() {
        testApply((src, configurer) -> src.applyFluent(srcWrapped -> configurer.apply(srcWrapped.unwrap()).toFluent()));
    }

    private void testApply(
            BiFunction<
                    FluentSeqGroupConsumer<String>,
                    Function<SeqGroupConsumer<String>, SeqGroupConsumer<Long>>,
                    FluentSeqGroupConsumer<Long>
                    > applier
    ) {
        SeqGroupConsumer<String> srcConsumer = (cancelToken, seqGroupProducer) -> {
            System.out.println("testApply.a");
        };
        SeqGroupConsumer<Long> newConsumer = (cancelToken, seqGroupProducer) -> {
            System.out.println("testApply.b");
        };

        SeqGroupConsumer<Long> received = applier
                .apply(srcConsumer.toFluent(), prev -> {
                    assertSame(srcConsumer, prev);
                    return newConsumer;
                })
                .unwrap();

        assertSame(newConsumer, received);
    }

    private static SeqGroupProducer<String> testSrc() {
        return FluentSeqGroupProducerTest.iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList()
        );
    }

    @Test
    public void testThenForGroups() throws Exception {
        List<List<String>> result = new ArrayList<>();

        AtomicInteger orderRef = new AtomicInteger(0);
        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x." + orderRef.getAndIncrement());
        SeqGroupConsumer<String> consumer2 = collectingConsumer(result, e -> e + "y." + orderRef.getAndIncrement());

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenForGroups(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax.0", "bx.2", "cx.4"),
                Arrays.asList("ay.1", "by.3", "cy.5"),
                Arrays.asList("dx.6", "ex.8"),
                Arrays.asList("dy.7", "ey.9"),
                Arrays.asList(),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenForGroupsNoOpFirst() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        SeqGroupConsumer<String> consumer2 = collectingConsumer(result, e -> e + "y");

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenForGroups(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ay", "by", "cy"),
                Arrays.asList("dy", "ey"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenForGroupsNoOpSecond() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x");
        SeqGroupConsumer<String> consumer2 = SeqGroupConsumer.draining();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenForGroups(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "bx", "cx"),
                Arrays.asList("dx", "ex"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenForGroupsNoOpAll() {
        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        SeqGroupConsumer<String> consumer2 = SeqGroupConsumer.draining();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenForGroups(consumer2.toFluent())
                .unwrap();

        assertSame(SeqGroupConsumer.draining(), combined);
    }

    @Test
    public void testThen() throws Exception {
        List<List<String>> result = new ArrayList<>();

        AtomicInteger orderRef = new AtomicInteger(0);
        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x." + orderRef.getAndIncrement());
        SeqConsumer<String> consumer2 = collectingBasicConsumer(result, e -> e + "y." + orderRef.getAndIncrement());

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .then(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax.0", "bx.2", "cx.4"),
                Arrays.asList("ay.1", "by.3", "cy.5"),
                Arrays.asList("dx.6", "ex.8"),
                Arrays.asList("dy.7", "ey.9"),
                Arrays.asList(),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenNoOpFirst() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        SeqConsumer<String> consumer2 = collectingBasicConsumer(result, e -> e + "y");

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .then(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ay", "by", "cy"),
                Arrays.asList("dy", "ey"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenNoOpSecond() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x");
        SeqConsumer<String> consumer2 = SeqConsumer.draining();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .then(consumer2.toFluent())
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "bx", "cx"),
                Arrays.asList("dx", "ex"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenNoOpAll() {
        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        SeqConsumer<String> consumer2 = SeqConsumer.draining();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .then(consumer2.toFluent())
                .unwrap();

        assertSame(SeqGroupConsumer.draining(), combined);
    }

    @Test
    public void testThenContextFree() throws Exception {
        List<List<String>> result = new ArrayList<>();

        AtomicInteger orderRef = new AtomicInteger(0);
        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x." + orderRef.getAndIncrement());

        List<String> result2 = new ArrayList<>();
        ElementConsumer<String> consumer2 = e -> result2.add(e + "y." + orderRef.getAndIncrement());

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenContextFree(consumer2)
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax.0", "bx.2", "cx.4"),
                Arrays.asList("dx.6", "ex.8"),
                Arrays.asList()
        );
        assertEquals(expected, result);

        List<String> expected2 = Arrays.asList("ay.1", "by.3", "cy.5", "dy.7", "ey.9");
        assertEquals(expected2, result2);
    }

    @Test
    public void testThenContextFreeNoOpFirst() throws Exception {
        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        List<String> result = new ArrayList<>();
        ElementConsumer<String> consumer2 = e -> result.add(e + "y");

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenContextFree(consumer2)
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<String> expected = Arrays.asList("ay", "by", "cy", "dy", "ey");
        assertEquals(expected, result);
    }

    @Test
    public void testThenContextFreeNoOpSecond() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupConsumer<String> consumer1 = collectingConsumer(result, e -> e + "x");
        ElementConsumer<String> consumer2 = ElementConsumer.noOp();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenContextFree(consumer2)
                .unwrap();

        combined.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "bx", "cx"),
                Arrays.asList("dx", "ex"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testThenContextFreeNoOpAll() {
        SeqGroupConsumer<String> consumer1 = SeqGroupConsumer.draining();
        ElementConsumer<String> consumer2 = ElementConsumer.noOp();

        SeqGroupConsumer<String> combined = consumer1.toFluent()
                .thenContextFree(consumer2)
                .unwrap();

        assertSame(SeqGroupConsumer.draining(), combined);
    }

    private void testInBackground(
            Function<FluentSeqGroupConsumer<String>, FluentSeqGroupConsumer<String>> inBackground,
            Consumer<? super String> peekAction) throws Exception {

        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        List<String> result = new ArrayList<>();

        SeqGroupConsumer<String> src = ElementConsumers.contextFreeSeqGroupConsumer(e -> {
            try {
                result.add(e);
                peekAction.accept(e);
            } catch (Throwable ex) {
                setFirstException(testErrorRef, "peek error", ex);
            }
        });

        SeqGroupConsumer<String> consumer = inBackground
                .apply(src.toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());
        verifyNoException(testErrorRef);
        assertEquals(expected, result);
    }

    @Test(timeout = 10000)
    public void testInBackgroundOwned() throws Exception {
        String executorName = "Test-Executor-testInBackgroundOwned";
        testInBackground(
                consumer -> consumer.inBackground(executorName, 1, 0),
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
                    consumer -> consumer.inBackground(executor, 1, 0),
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

    @Test(timeout = 10000)
    public void testInBackgroundRetainSequencesOwned() throws Exception {
        String executorName = "Test-Executor-testInBackgroundOwned";
        testInBackground(
                consumer -> consumer.inBackgroundRetainSequences(executorName, 0),
                element -> {
                    String threadName = Thread.currentThread().getName();
                    if (!threadName.contains(executorName)) {
                        throw new IllegalStateException("Expected to run in background, but running in " + threadName);
                    }
                }
        );
    }

    @Test(timeout = 10000)
    public void testInBackgroundRetainSequencesExternal() throws Exception {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("Test-Executor-testInBackgroundExternal");
        try {
            testInBackground(
                    consumer -> consumer.inBackgroundRetainSequences(executor, 0),
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

    @Test
    public void testToInspectorMapper() throws Exception {
        List<List<String>> result = new ArrayList<>();
        SeqGroupConsumer<String> consumer = collectingConsumer(result, e -> e + "x");

        SeqGroupMapper<String, String> mapper = consumer.toFluent()
                .toInspectorMapper()
                .unwrap();

        mapper.mapAll(Cancellation.UNCANCELABLE_TOKEN, testSrc(), SeqGroupConsumer.draining());

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "bx", "cx"),
                Arrays.asList("dx", "ex"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testToInspectorMapperWithNoOp() {
        SeqGroupMapper<?, ?> mapper = SeqGroupConsumer.draining()
                .toFluent()
                .toInspectorMapper()
                .unwrap();
        assertSame(SeqGroupMapper.identity(), mapper);
    }

    private <T> void testConsumer(
            List<List<T>> expected,
            Function<FluentSeqGroupConsumer<T>, FluentSeqGroupConsumer<String>> applier
    ) throws Exception {
        List<List<T>> collected = new ArrayList<>();
        SeqGroupConsumer<String> consumer = applier
                .apply(collectingConsumer(collected).toFluent())
                .unwrap();

        testSrc()
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
                Arrays.asList(
                        Arrays.asList("a", "b", "c"),
                        Arrays.asList("d", "e"),
                        Arrays.asList()
                ),
                c -> c.mapped(SeqMapper.identity())
        );
    }

    @Test
    public void testMappedSeq() throws Exception {
        testConsumer(
                Arrays.asList(
                        wrapped("a", "b", "c"),
                        wrapped("d", "e"),
                        wrapped()
                ),
                c -> {
                    return c.mapped(SeqMapper
                            .fromElementMapper((e, consumer) -> {
                                consumer.processElement(new Wrapper(e));
                            })
                    );
                }
        );
    }

    @Test
    public void testMappedSeqFluent() throws Exception {
        testConsumer(
                Arrays.asList(
                        wrapped("a", "b", "c"),
                        wrapped("d", "e"),
                        wrapped()
                ),
                c -> {
                    return c.mapped(SeqMapper
                            .<String, Wrapper>fromElementMapper((e, consumer) -> {
                                consumer.processElement(new Wrapper(e));
                            })
                            .toFluent()
                    );
                }
        );
    }

    @Test
    public void testMappedContextFree() throws Exception {
        testConsumer(
                Arrays.asList(
                        wrapped("a", "b", "c"),
                        wrapped("d", "e"),
                        wrapped()
                ),
                c -> {
                    return c.mappedContextFree((e, consumer) -> {
                        consumer.processElement(new Wrapper(e));
                    });
                }
        );
    }

    @Test
    public void testMappedContextFreeIdentity() throws Exception {
        testConsumer(
                Arrays.asList(
                        Arrays.asList("a", "b", "c"),
                        Arrays.asList("d", "e"),
                        Arrays.asList()
                ),
                c -> c.mappedContextFree(ElementMapper.identity())
        );
    }

}
