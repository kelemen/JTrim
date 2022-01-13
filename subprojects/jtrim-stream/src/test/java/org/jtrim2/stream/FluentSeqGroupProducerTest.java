package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.jtrim2.stream.SeqGroupProducerTest.*;
import static org.junit.Assert.*;

public class FluentSeqGroupProducerTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(ElementProducers.class);
    }

    private static <T> List<List<T>> collectCanceled(
            int collectCount,
            SeqGroupProducer<? extends T> seqGroupProducer) throws Exception {

        CancellationSource cancellation = Cancellation.createCancellationSource();
        if (collectCount == 0) {
            cancellation.getController().cancel();
        }

        AtomicInteger totalResultSize = new AtomicInteger(0);
        List<List<T>> result = Collections.synchronizedList(new ArrayList<>());
        try {
            seqGroupProducer.transferAll(cancellation.getToken(), (consumerCancelToken, seqConsumer) -> {
                List<T> groupResult = new ArrayList<>();
                try {
                    seqConsumer.transferAll(consumerCancelToken, element -> {
                        consumerCancelToken.checkCanceled();
                        groupResult.add(element);
                        if (totalResultSize.incrementAndGet() == collectCount) {
                            cancellation.getController().cancel();
                        }
                    });
                } finally {
                    if (!groupResult.isEmpty()) {
                        result.add(groupResult);
                    }
                }
            });
        } catch (OperationCanceledException ex) {
            // Fine, the API is allowed to throw this
        }
        return new ArrayList<>(result);
    }

    private static <T> void assertCanceledCollecting(
            int collectCount,
            List<List<T>> expected,
            SeqGroupProducer<? extends T> producer) throws Exception {

        if (collectCount >= 0 && expected.stream().mapToInt(List::size).sum() >= collectCount) {
            List<List<T>> expectedSubList = new ArrayList<>();
            int rem = collectCount;
            for (List<T> part: expected) {
                if (part.size() < rem) {
                    expectedSubList.add(part);
                    rem -= part.size();
                } else {
                    if (rem > 0) {
                        expectedSubList.add(part.subList(0, rem));
                    }
                    break;
                }
            }

            assertEquals(expectedSubList, collectCanceled(collectCount, producer));
        }
    }

    private static <T> void assertContentAndCancellation(
            List<List<T>> expected,
            SeqGroupProducer<? extends T> producer) throws Exception {

        assertEquals(expected, collect(producer));

        assertCanceledCollecting(0, expected, producer);
        assertCanceledCollecting(1, expected, producer);
        assertCanceledCollecting(2, expected, producer);
        assertCanceledCollecting(expected.size() - 1, expected, producer);
        assertCanceledCollecting(expected.size(), expected, producer);
    }

    private static void expectCancellation(
            CancellationToken cancelToken,
            AtomicReference<? super RuntimeException> errorRef) {

        if (!cancelToken.isCanceled()) {
            setFirstException(errorRef, "Expected cancellation detection.");
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> SeqGroupProducer<T> iterableProducer(Iterable<? extends T>... src) {
        return manyIterableProducer(Arrays.asList(src));
    }

    static <T> SeqGroupProducer<T> manyIterableProducer(Iterable<? extends Iterable<? extends T>> src) {
        List<SeqProducer<T>> seqProducers = new ArrayList<>();
        for (Iterable<? extends T> group: src) {
            seqProducers.add(SeqProducer.iterableProducer(group));
        }

        return (CancellationToken cancelToken, SeqConsumer<? super T> seqConsumer) -> {
            for (SeqProducer<T> seqProducer: seqProducers) {
                seqConsumer.consumeAll(cancelToken, seqProducer);
            }
        };
    }

    static SeqGroupMapper<String, String> testSeqGroupMapper(String... suffixes) {
        SeqMapper<String, String> mapper = FluentSeqProducerTest.testSeqMapper(suffixes);

        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            seqGroupConsumer.consumeAll(cancelToken, (producerCancelToken, seqConsumer) -> {
                seqGroupProducer.transferAll(producerCancelToken, (consumerCancelToken, seqProducer) -> {
                    mapper.mapAll(consumerCancelToken, seqProducer, seqConsumer);
                });
            });
        };
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static <T> List<List<T>> singleGroupResult(T... values) {
        return Arrays.asList(Arrays.asList(values.clone()));
    }

    @Test
    public void testMapGroupsEmpty() {
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .mapGroups(testSeqGroupMapper("x", "y"))
                .unwrap();
        assertSame(SeqGroupProducer.<String>empty(), producer);
    }

    @Test
    public void testIdentityMapGroups() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapGroups(SeqGroupMapper.identity())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testMapGroupsSingleGroup() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapGroups(testSeqGroupMapper("x", "y"))
                .unwrap();

        assertEquals(singleGroupResult("ax", "ay", "bx", "by", "cx", "cy"), collect(producer));
    }

    @Test
    public void testMapGroupsManyGroup() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .mapGroups(testSeqGroupMapper("x", "y"))
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"),
                Arrays.asList(),
                Arrays.asList("dx", "dy", "ex", "ey")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testMapGroupsNoGroup() throws Exception {
        SeqGroupProducer<String> producer = FluentSeqGroupProducerTest.<String>iterableProducer()
                .toFluent()
                .mapGroups(testSeqGroupMapper("x", "y"))
                .unwrap();

        assertEquals(Arrays.asList(), collect(producer));
    }

    @Test
    public void testMapEmpty() {
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .map(FluentSeqProducerTest.testSeqMapper("x", "y"))
                .unwrap();
        assertSame(SeqGroupProducer.<String>empty(), producer);
    }

    @Test
    public void testIdentityMap() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .map(SeqMapper.identity())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testMapSingleGroup() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .map(FluentSeqProducerTest.testSeqMapper("x", "y"))
                .unwrap();

        assertEquals(singleGroupResult("ax", "ay", "bx", "by", "cx", "cy"), collect(producer));
    }

    @Test
    public void testMapManyGroup() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .map(FluentSeqProducerTest.testSeqMapper("x", "y"))
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"),
                Arrays.asList(),
                Arrays.asList("dx", "dy", "ex", "ey")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testMapNoGroup() throws Exception {
        SeqGroupProducer<String> producer = FluentSeqGroupProducerTest.<String>iterableProducer()
                .toFluent()
                .map(FluentSeqProducerTest.testSeqMapper("x", "y"))
                .unwrap();

        assertEquals(Arrays.asList(), collect(producer));
    }

    @Test
    public void testMapContextFreeEmpty() {
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .mapContextFree(FluentSeqProducerTest.testMapper("x", "y"))
                .unwrap();
        assertSame(SeqGroupProducer.<String>empty(), producer);
    }

    @Test
    public void testIdentityMapContextFree() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapContextFree(ElementMapper.identity())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testMapContextFreeSingleGroup() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapContextFree(FluentSeqProducerTest.testMapper("x", "y"))
                .unwrap();

        assertEquals(singleGroupResult("ax", "ay", "bx", "by", "cx", "cy"), collect(producer));
    }

    @Test
    public void testMapContextFreeManyGroup() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .mapContextFree(FluentSeqProducerTest.testMapper("x", "y"))
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"),
                Arrays.asList(),
                Arrays.asList("dx", "dy", "ex", "ey")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testMapContextFreeNoGroup() throws Exception {
        SeqGroupProducer<String> producer = FluentSeqGroupProducerTest.<String>iterableProducer()
                .toFluent()
                .mapContextFree(FluentSeqProducerTest.testMapper("x", "y"))
                .unwrap();

        assertEquals(Arrays.asList(), collect(producer));
    }

    @Test
    public void testBatch0() {
        FluentSeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent();

        try {
            producer.batch(0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("0"));
        }
    }

    private static SeqGroupProducer<List<String>> testBatchProducer(
            int batchSize,
            String... element) {

        return iterableProducer(Arrays.asList(element))
                .toFluent()
                .batch(batchSize)
                .unwrap();
    }

    @Test
    public void testBatchEmpty() {
        SeqProducer<List<String>> producer = SeqProducer.<String>empty()
                .toFluent()
                .batch(2)
                .unwrap();
        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testBatch1() throws Exception {
        SeqGroupProducer<List<String>> producer = testBatchProducer(1, "a", "b", "c");
        assertEquals(singleGroupResult(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")), collect(producer));
    }

    @Test
    public void testBatch2() throws Exception {
        SeqGroupProducer<List<String>> producer = testBatchProducer(2, "a", "b", "c");
        assertEquals(singleGroupResult(Arrays.asList("a", "b"), Arrays.asList("c")), collect(producer));
    }

    @Test
    public void testExactBatch() throws Exception {
        SeqGroupProducer<List<String>> producer = testBatchProducer(3, "a", "b", "c");
        assertEquals(singleGroupResult(Arrays.asList("a", "b", "c")), collect(producer));
    }

    @Test
    public void testBatchLargerThanSize() throws Exception {
        SeqGroupProducer<List<String>> producer = testBatchProducer(4, "a", "b", "c");
        assertEquals(singleGroupResult(Arrays.asList("a", "b", "c")), collect(producer));
    }

    @Test
    public void testBatchMultiGroup() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d"),
                Arrays.asList(),
                Arrays.asList("e", "f", "g", "h")
        );
        SeqGroupProducer<List<String>> producer = src
                .toFluent()
                .batch(2)
                .unwrap();

        List<List<List<String>>> expected = Arrays.asList(
                Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c")),
                Arrays.asList(Arrays.asList("d")),
                Arrays.asList(),
                Arrays.asList(Arrays.asList("e", "f"), Arrays.asList("g", "h"))
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testBatchCancelable() throws Exception {
        SeqGroupProducer<List<String>> producer = iterableProducer(Arrays.asList("a", "b", "c", "d", "e"))
                .toFluent()
                .batch(2)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d"),
                Arrays.asList("e")
        );
        assertContentAndCancellation(Arrays.asList(expected), producer);
    }

    @Test
    public void testPostPeekContextFreeEmpty() {
        // This currently does not have a public access, but add it for coverage anyway.
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = ElementProducers.postPeekedSeqGroupProducer(
                SeqGroupProducer.empty(),
                SeqConsumerTest.collectingConsumer(peeked)
        );

        assertSame(SeqGroupProducer.empty(), producer);
    }

    @Test
    public void testPostPeekContextFreeNoOp() throws Exception {
        // This currently does not have a public access, but add it for coverage anyway.
        SeqGroupProducer<String> src = iterableProducer(Arrays.asList("a", "b", "c"));
        SeqGroupProducer<String> producer = ElementProducers.postPeekedSeqGroupProducer(src, SeqConsumer.draining());

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeekEmpty() {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .peek(SeqConsumerTest.collectingConsumer(peeked))
                .unwrap();

        assertSame(SeqGroupProducer.empty(), producer);
        assertEquals(Collections.emptyList(), peeked);
    }

    @Test
    public void testPeekNoOp() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek(SeqConsumer.draining())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeek() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek(SeqConsumerTest.collectingConsumer(peeked))
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekMultiGroup() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peek(SeqConsumerTest.collectingConsumer(peeked))
                .unwrap();

        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e")), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), peeked);
    }

    @Test
    public void testPeekHappensBeforeNextStep() throws Exception {
        List<String> peeked = new ArrayList<>();
        List<StringBuilder> src = Arrays.asList(
                new StringBuilder("a"),
                new StringBuilder("b"),
                new StringBuilder("c")
        );
        SeqGroupProducer<String> producer = iterableProducer(src)
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, element -> {
                        peeked.add(element.toString());
                        element.append("x");
                    });
                })
                .<String>mapContextFree((element, consumer) -> consumer.processElement(element.toString()))
                .unwrap();

        assertEquals(singleGroupResult("ax", "bx", "cx"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekCancelable() throws Exception {
        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );
        CancellationSource cancellation = Cancellation.createCancellationSource();
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, element -> {
                        peeked.add(element);
                        if (peeked.size() == 4) {
                            cancellation.getController().cancel();
                            expectCancellation(cancelToken, testErrorRef);
                        }
                    });
                })
                .unwrap();

        try {
            collect(cancellation.getToken(), producer);
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }
        verifyNoException(testErrorRef);
        assertEquals(Arrays.asList("a", "b", "c", "d"), peeked);
    }

    private static <T> SeqGroupConsumer<T> listCollectorSeqGroupConsumer(List<List<T>> result) {
        return (cancelToken, seqGroupProducer) -> {
            seqGroupProducer.transferAll(cancelToken, (seqConsumerCancelToken, seqProducer) -> {
                List<T> groupResult = new ArrayList<>();
                result.add(groupResult);
                SeqConsumerTest.collectingConsumer(groupResult).consumeAll(seqConsumerCancelToken, seqProducer);
            });
        };
    }

    @Test
    public void testPeekGroupsEmpty() {
        List<List<String>> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .peekGroups(listCollectorSeqGroupConsumer(peeked))
                .unwrap();

        assertSame(SeqGroupProducer.empty(), producer);
        assertEquals(Collections.emptyList(), peeked);
    }

    @Test
    public void testPeekGroupsNoOp() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekGroups(SeqGroupConsumer.draining())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeekGroups() throws Exception {
        List<List<String>> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekGroups(listCollectorSeqGroupConsumer(peeked))
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
        assertEquals(singleGroupResult("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekGroupsMultiGroup() throws Exception {
        List<List<String>> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peekGroups(listCollectorSeqGroupConsumer(peeked))
                .unwrap();

        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e"));
        assertEquals(expected, collect(producer));
        assertEquals(expected, peeked);
    }

    @Test
    public void testPeekGroupsHappensBeforeNextStep() throws Exception {
        List<List<String>> peeked = new ArrayList<>();
        List<StringBuilder> src = Arrays.asList(
                new StringBuilder("a"),
                new StringBuilder("b"),
                new StringBuilder("c")
        );
        SeqGroupProducer<String> producer = iterableProducer(src)
                .toFluent()
                .peekGroups((cancelToken, seqGroupProducer) -> {
                    seqGroupProducer.transferAll(cancelToken, (seqConsumerCancelToken, seqProducer) -> {
                        List<String> groupPeeked = new ArrayList<>();
                        peeked.add(groupPeeked);
                        seqProducer.transferAll(cancelToken, element -> {
                            groupPeeked.add(element.toString());
                            element.append("x");
                        });
                    });
                })
                .<String>mapContextFree((element, consumer) -> consumer.processElement(element.toString()))
                .unwrap();

        assertEquals(singleGroupResult("ax", "bx", "cx"), collect(producer));
        assertEquals(singleGroupResult("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekGroupsCancelable() throws Exception {
        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        AtomicInteger peekCount = new AtomicInteger(0);
        List<List<String>> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );
        CancellationSource cancellation = Cancellation.createCancellationSource();
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peekGroups((cancelToken, seqGroupProducer) -> {
                    seqGroupProducer.transferAll(cancelToken, (seqConsumerCancelToken, seqProducer) -> {
                        List<String> groupPeeked = new ArrayList<>();
                        peeked.add(groupPeeked);
                        seqProducer.transferAll(seqConsumerCancelToken, element -> {
                            groupPeeked.add(element);
                            if (peekCount.incrementAndGet() == 4) {
                                cancellation.getController().cancel();
                                expectCancellation(cancelToken, testErrorRef);
                                expectCancellation(seqConsumerCancelToken, testErrorRef);
                            }
                        });
                    });
                })
                .unwrap();

        try {
            collect(cancellation.getToken(), producer);
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }
        verifyNoException(testErrorRef);
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d")), peeked);
    }

    private static <T> ElementConsumer<T> listCollectorConsumer(List<? super T> result) {
        return result::add;
    }

    @Test
    public void testPeekContextFreeEmpty() {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = SeqGroupProducer.<String>empty()
                .toFluent()
                .peekContextFree(listCollectorConsumer(peeked))
                .unwrap();

        assertSame(SeqGroupProducer.empty(), producer);
        assertEquals(Collections.emptyList(), peeked);
    }

    @Test
    public void testPeekContextFreeNoOp() throws Exception {
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(ElementConsumer.noOp())
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeekContextFree() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> producer = iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(listCollectorConsumer(peeked))
                .unwrap();

        assertEquals(singleGroupResult("a", "b", "c"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekContextFreeMultiGroup() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peekContextFree(listCollectorConsumer(peeked))
                .unwrap();

        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e")), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), peeked);
    }

    @Test
    public void testPeekContextFreeHappensBeforeNextStep() throws Exception {
        List<String> peeked = new ArrayList<>();
        List<StringBuilder> src = Arrays.asList(
                new StringBuilder("a"),
                new StringBuilder("b"),
                new StringBuilder("c")
        );
        SeqGroupProducer<String> producer = iterableProducer(src)
                .toFluent()
                .peekContextFree(element -> {
                    peeked.add(element.toString());
                    element.append("x");
                })
                .<String>mapContextFree((element, consumer) -> consumer.processElement(element.toString()))
                .unwrap();

        assertEquals(singleGroupResult("ax", "bx", "cx"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekContextFreeCancelable() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );
        CancellationSource cancellation = Cancellation.createCancellationSource();
        SeqGroupProducer<String> producer = src
                .toFluent()
                .peekContextFree(element -> {
                    peeked.add(element);
                    if (peeked.size() == 4) {
                        cancellation.getController().cancel();
                    }
                })
                .unwrap();

        try {
            collect(cancellation.getToken(), producer);
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), peeked);
    }

    private void testToBackground(
            boolean retainGroups,
            Function<FluentSeqGroupProducer<String>, FluentSeqGroupProducer<String>> toBackground,
            Consumer<? super String> peekAction) throws Exception {

        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        AtomicInteger peekCount = new AtomicInteger(0);
        SeqGroupProducer<String> producer = toBackground
                .apply(src.toFluent())
                .peekContextFree(element -> {
                    peekCount.incrementAndGet();
                    try {
                        peekAction.accept(element);
                    } catch (Throwable ex) {
                        setFirstException(testErrorRef, "peek error", ex);
                    }
                })
                .unwrap();


        List<List<String>> expected;
        if (retainGroups) {
            expected = Arrays.asList(
                    Arrays.asList("a", "b", "c"),
                    Arrays.asList("d", "e"),
                    Arrays.asList("f")
            );
        } else {
            expected = singleGroupResult("a", "b", "c", "d", "e", "f");
        }
        assertEquals(expected, collect(producer));
        assertEquals(6, peekCount.get());
        verifyNoException(testErrorRef);
    }

    @Test
    public void testToBackgroundOwned() throws Exception {
        String executorName = "Test-Executor-testToBackGroundOwned";
        testToBackground(
                false,
                producer -> producer.toBackground(executorName, 1, 0),
                element -> {
                    String threadName = Thread.currentThread().getName();
                    if (!threadName.contains(executorName)) {
                        throw new IllegalStateException("Expected to run in background, but running in " + threadName);
                    }
                }
        );
    }

    @Test
    public void testToBackgroundExternal() throws Exception {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("Test-Executor-testToBackGroundExternal");
        try {
            testToBackground(
                    false,
                    producer -> producer.toBackground(executor, 1, 0),
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
    public void testToBackgroundRetainSequencesOwned() throws Exception {
        String executorName = "Test-Executor-testToBackgroundRetainSequencesOwned";
        testToBackground(
                true,
                producer -> producer.toBackgroundRetainSequences(executorName, 0),
                element -> {
                    String threadName = Thread.currentThread().getName();
                    if (!threadName.contains(executorName)) {
                        throw new IllegalStateException("Expected to run in background, but running in " + threadName);
                    }
                }
        );
    }

    @Test
    public void testToBackgroundRetainSequencesExternal() throws Exception {
        SingleThreadedExecutor executor
                = new SingleThreadedExecutor("Test-Executor-testToBackgroundRetainSequencesExternal");

        try {
            testToBackground(
                    true,
                    producer -> producer.toBackgroundRetainSequences(executor, 0),
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
    public void testToSynchronized() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        List<String> result = src
                .toFluent()
                .toSynchronized()
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testToSynchronizedEmpty() {
        SeqGroupProducer<String> src = SeqGroupProducer.empty();

        SeqProducer<String> syncProducer = src
                .toFluent()
                .toSynchronized()
                .unwrap();
        assertSame(SeqProducer.empty(), syncProducer);
    }

    @Test
    public void testWithConsumer() throws Exception {
        List<List<String>> result = new ArrayList<>();

        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        src.toFluent()
                .withConsumer((cancelToken, seqGroupProducer) -> {
                    seqGroupProducer.transferAll(cancelToken, (seqConsumerCancelToken, seqProducer) -> {
                        List<String> groupResult = new ArrayList<>();
                        result.add(groupResult);
                        seqProducer.transferAll(seqConsumerCancelToken, groupResult::add);
                    });
                })
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        assertEquals(expected, result);
    }

    @Test
    public void testWithConsumerCancelable() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        List<List<String>> result = new ArrayList<>();

        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        AtomicInteger consumeCountRef = new AtomicInteger(0);
        CancelableTask consumeTask = src
                .toFluent()
                .withConsumer((cancelToken, seqGroupProducer) -> {
                    seqGroupProducer.transferAll(cancelToken, (seqConsumerCancelToken, seqProducer) -> {
                        List<String> groupResult = new ArrayList<>();
                        result.add(groupResult);
                        seqProducer.transferAll(seqConsumerCancelToken, element -> {
                            seqConsumerCancelToken.checkCanceled();
                            groupResult.add(element);
                            if (consumeCountRef.incrementAndGet() == 4) {
                                cancellation.getController().cancel();
                                expectCancellation(cancelToken, testErrorRef);
                                expectCancellation(seqConsumerCancelToken, testErrorRef);
                            }
                        });
                    });
                });

        try {
            consumeTask.execute(cancellation.getToken());
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }

        verifyNoException(testErrorRef);

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d")
        );
        assertEquals(expected, result);
    }

    @Test
    public void testWithContextFreeSeqConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withContextFreeSeqConsumer(SeqConsumerTest.collectingConsumer(result))
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testWithSingleShotSeqConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withSingleShotSeqConsumer(SeqConsumerTest.collectingConsumer(result))
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testWithSingleShotSeqConsumerMultiCall() throws Exception {
        List<String> result = new ArrayList<>();

        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d")
        );

        CancelableTask task = src
                .toFluent()
                .withSingleShotSeqConsumer(SeqConsumerTest.collectingConsumer(result));

        try {
            task.execute(Cancellation.UNCANCELABLE_TOKEN);
            fail("Expected failure");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("multiple"));
        }

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testWithContextFreeConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withContextFreeConsumer(result::add)
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testCollect() throws Exception {
        SeqGroupProducer<String> src = iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );

        List<String> result = src.toFluent()
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testCollectConcurrent() throws Exception {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("Test-Thread-testCollectConcurrent", threadCount);
        try {
            List<List<String>> srcElement = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                srcElement.add(Arrays.asList(i + ".a", i + ".b"));
            }
            srcElement = Collections.unmodifiableList(srcElement);

            List<String> expected = srcElement
                    .stream()
                    .flatMap(List::stream)
                    .sorted()
                    .collect(Collectors.toList());

            for (int i = 0; i < 100; i++) {
                Collection<String> result = manyIterableProducer(srcElement)
                        .toFluent()
                        .toBackground(executor, threadCount, 0)
                        .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toCollection(ConcurrentLinkedQueue::new));

                List<String> sortedResult = new ArrayList<>(result);
                sortedResult.sort(null);
                assertEquals(expected, sortedResult);
            }
        } finally {
            executor.shutdownAndCancel();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test
    public void testCollectEmpty() throws Exception {
        List<String> result = SeqGroupProducer.<String>empty().toFluent()
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());

        assertEquals(Arrays.asList(), result);
    }

    private static SeqGroupProducer<String> testLimitSrc() {
        return iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
    }

    @Test
    public void testLimit0() throws Exception {
        SeqGroupProducer<String> producer = testLimitSrc()
                .toFluent()
                .limitEachSequence(0)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList()
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testLimit1() throws Exception {
        SeqGroupProducer<String> producer = testLimitSrc()
                .toFluent()
                .limitEachSequence(1)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a"),
                Arrays.asList(),
                Arrays.asList("d")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testLimitSimple() throws Exception {
        SeqGroupProducer<String> producer = testLimitSrc()
                .toFluent()
                .limitEachSequence(2)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testLimitExact() throws Exception {
        SeqGroupProducer<String> producer = testLimitSrc()
                .toFluent()
                .limitEachSequence(3)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        assertEquals(expected, collect(producer));
    }

    @Test
    public void testLimitMore() throws Exception {
        SeqGroupProducer<String> producer = testLimitSrc()
                .toFluent()
                .limitEachSequence(4)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c"),
                Arrays.asList(),
                Arrays.asList("d", "e")
        );
        assertEquals(expected, collect(producer));
    }
}
