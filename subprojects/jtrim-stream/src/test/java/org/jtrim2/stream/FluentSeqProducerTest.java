package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.jtrim2.stream.SeqProducerTest.*;
import static org.junit.Assert.*;

public class FluentSeqProducerTest {
    private static <T> List<T> collectCanceled(
            int collectCount,
            SeqProducer<? extends T> seqProducer) throws Exception {

        CancellationSource cancellation = Cancellation.createCancellationSource();
        if (collectCount == 0) {
            cancellation.getController().cancel();
        }

        List<T> result = new ArrayList<>();
        try {
            seqProducer.transferAll(cancellation.getToken(), element -> {
                result.add(element);
                if (result.size() == collectCount) {
                    cancellation.getController().cancel();
                }
            });
        } catch (OperationCanceledException ex) {
            // Fine, the API is allowed to throw this
        }
        return result;
    }

    static <T> SeqProducer<T> cancelableIterableProducer(Iterable<? extends T> src) {
        Objects.requireNonNull(src, "src");

        return (cancelToken, consumer) -> {
            src.forEach(element -> {
                try {
                    cancelToken.checkCanceled();
                    consumer.processElement(element);
                } catch (Exception ex) {
                    throw ExceptionHelper.throwUnchecked(ex);
                }
            });
        };
    }

    private static <T> void assertCanceledCollecting(
            int collectCount,
            List<T> expected,
            SeqProducer<? extends T> producer) throws Exception {

        if (expected.size() >= collectCount && collectCount >= 0) {
            assertEquals(expected.subList(0, collectCount), collectCanceled(collectCount, producer));
        }
    }

    private static <T> void assertContentAndCancellation(
            List<T> expected,
            SeqProducer<? extends T> producer) throws Exception {

        assertEquals(expected, collect(producer));

        assertCanceledCollecting(0, expected, producer);
        assertCanceledCollecting(1, expected, producer);
        assertCanceledCollecting(2, expected, producer);
        assertCanceledCollecting(expected.size() - 1, expected, producer);
        assertCanceledCollecting(expected.size(), expected, producer);
    }

    private void testConcat(List<String> first, List<String> second) throws Exception {
        testConcatUncancelable(first, second);
        testConcatCancelable(first, second);
    }

    private void testConcatUncancelable(List<String> first, List<String> second) throws Exception {
        SeqProducer<String> combined = SeqProducer.iterableProducer(first)
                .toFluent()
                .concat(SeqProducer.iterableProducer(second))
                .unwrap();

        assertEquals(CollectionsEx.viewConcatList(first, second), collect(combined));
    }

    private void testConcatCancelable(List<String> first, List<String> second) throws Exception {
        SeqProducer<String> combined = cancelableIterableProducer(first)
                .toFluent()
                .concat(cancelableIterableProducer(second))
                .unwrap();

        assertContentAndCancellation(new ArrayList<>(CollectionsEx.viewConcatList(first, second)), combined);
    }

    @Test
    public void testConcat() throws Exception {
        testConcat(Arrays.asList("a", "b", "c"), Arrays.asList("d", "e"));
        testConcat(Arrays.asList("a", "b", "c"), Arrays.asList());
        testConcat(Arrays.asList(), Arrays.asList("d", "e"));
        testConcat(Arrays.asList(), Arrays.asList());
    }

    static ElementMapper<String, String> testMapper(String... suffixes) {
        return testMapper(Cancellation.UNCANCELABLE_TOKEN, suffixes);
    }

    private static ElementMapper<String, String> testMapper(CancellationToken cancelToken, String... suffixes) {
        String[] suffixesCopy = suffixes.clone();
        return (element, consumer) -> {
            for (String suffix: suffixesCopy) {
                cancelToken.checkCanceled();
                consumer.processElement(element + suffix);
            }
        };
    }

    static SeqMapper<String, String> testSeqMapper(String... suffixes) {
        String[] suffixesCopy = suffixes.clone();
        return (cancelToken, seqProducer, seqConsumer) -> {
            seqConsumer.consumeAll(cancelToken, (producerCancelToken, consumer) -> {
                ElementMapper<String, String> mapper = testMapper(producerCancelToken, suffixesCopy);
                seqProducer.transferAll(producerCancelToken, element -> {
                    mapper.map(element, consumer);
                });
            });
        };
    }

    @Test
    public void testMapEmpty() throws Exception {
        SeqProducer<String> producer = SeqProducer.<String>empty()
                .toFluent()
                .map(testSeqMapper("x", "y"))
                .unwrap();
        assertSame(SeqProducer.<String>empty(), producer);
    }

    @Test
    public void testIdentityMap() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .map(SeqMapper.identity())
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testMap() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .map(testSeqMapper("x", "y"))
                .unwrap();

        assertEquals(Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"), collect(producer));
    }

    @Test
    public void testMapCancelable() throws Exception {
        SeqProducer<String> producer = cancelableIterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .map(testSeqMapper("x", "y"))
                .unwrap();

        assertContentAndCancellation(Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"), producer);
    }

    @Test
    public void testMapContextFreeEmpty() throws Exception {
        SeqProducer<String> producer = SeqProducer.<String>empty()
                .toFluent()
                .mapContextFree(testMapper("x", "y"))
                .unwrap();
        assertSame(SeqProducer.<String>empty(), producer);
    }

    @Test
    public void testIdentityMapContextFree() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapContextFree(ElementMapper.identity())
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testMapContextFree() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapContextFree(testMapper("x", "y"))
                .unwrap();

        assertEquals(Arrays.asList("ax", "ay", "bx", "by", "cx", "cy"), collect(producer));
    }

    @Test
    public void testMapContextFreeCancelable() throws Exception {
        SeqProducer<String> producer = cancelableIterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .mapContextFree(testMapper("x"))
                .unwrap();

        assertContentAndCancellation(Arrays.asList("ax", "bx", "cx"), producer);
    }

    @Test
    public void testBatch0() throws Exception {
        FluentSeqProducer<String> producer = SeqProducer
                .iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent();

        try {
            producer.batch(0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("0"));
        }
    }

    private static SeqProducer<List<String>> testBatchProducer(
            int batchSize,
            String... element) {

        return SeqProducer
                .iterableProducer(Arrays.asList(element))
                .toFluent()
                .batch(batchSize)
                .unwrap();
    }

    @Test
    public void testBatchEmpty() throws Exception {
        SeqProducer<List<String>> producer = SeqProducer.<String>empty()
                .toFluent()
                .batch(2)
                .unwrap();
        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testBatch1() throws Exception {
        SeqProducer<List<String>> producer = testBatchProducer(1, "a", "b", "c");
        assertEquals(Arrays.asList(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")), collect(producer));
    }

    @Test
    public void testBatch2() throws Exception {
        SeqProducer<List<String>> producer = testBatchProducer(2, "a", "b", "c");
        assertEquals(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c")), collect(producer));
    }

    @Test
    public void testExactBatch() throws Exception {
        SeqProducer<List<String>> producer = testBatchProducer(3, "a", "b", "c");
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c")), collect(producer));
    }

    @Test
    public void testBatchLargerThanSize() throws Exception {
        SeqProducer<List<String>> producer = testBatchProducer(4, "a", "b", "c");
        assertEquals(Arrays.asList(Arrays.asList("a", "b", "c")), collect(producer));
    }

    @Test
    public void testBatchCancelable() throws Exception {
        SeqProducer<List<String>> producer = cancelableIterableProducer(Arrays.asList("a", "b", "c", "d", "e"))
                .toFluent()
                .batch(2)
                .unwrap();

        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d"),
                Arrays.asList("e")
        );
        assertContentAndCancellation(expected, producer);
    }

    @Test
    public void testPeekEmpty() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = SeqProducer.<String>empty()
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, peeked::add);
                })
                .unwrap();

        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testPeekNoOp() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek(SeqConsumer.draining())
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeek() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, peeked::add);
                })
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekHappensBeforeNextStep() throws Exception {
        List<String> peeked = new ArrayList<>();
        List<StringBuilder> src = Arrays.asList(
                new StringBuilder("a"),
                new StringBuilder("b"),
                new StringBuilder("c")
        );
        SeqProducer<String> producer = SeqProducer.iterableProducer(src)
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, element -> {
                        peeked.add(element.toString());
                        element.append("x");
                    });
                })
                .<String>mapContextFree((element, consumer) -> consumer.processElement(element.toString()))
                .unwrap();

        assertEquals(Arrays.asList("ax", "bx", "cx"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekCancelable1() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, element -> {
                        cancelToken.checkCanceled();
                        peeked.add(element);
                    });
                })
                .unwrap();

        List<String> expected = Arrays.asList("a", "b", "c");
        assertCanceledCollecting(1, expected, producer);
        assertEquals(Arrays.asList("a"), peeked);
    }

    @Test
    public void testPeekCancelable2() throws Exception {
        SeqProducer<String> producer = cancelableIterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peek((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, ElementConsumer.noOp());
                })
                .unwrap();

        assertContentAndCancellation(Arrays.asList("a", "b", "c"), producer);
    }

    @Test
    public void testPostPeekContextFreeEmpty() throws Exception {
        // This currently does not have a public access, but add it for coverage anyway.
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = ElementProducers
                .postPeekedSeqProducerContextFree(SeqProducer.<String>empty(), peeked::add);

        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testPostPeekContextFreeNoOp() throws Exception {
        // This currently does not have a public access, but add it for coverage anyway.
        SeqProducer<String> src = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"));
        SeqProducer<String> producer = ElementProducers.postPeekedSeqProducerContextFree(src, ElementConsumer.noOp());

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeekContextFreeEmpty() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = SeqProducer.<String>empty()
                .toFluent()
                .peekContextFree(peeked::add)
                .unwrap();

        assertSame(SeqProducer.empty(), producer);
    }

    @Test
    public void testPeekContextFreeNoOp() throws Exception {
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(ElementConsumer.noOp())
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testPeekContextFree() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(peeked::add)
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekContextFreeHappensBeforeNextStep() throws Exception {
        List<String> peeked = new ArrayList<>();
        List<StringBuilder> src = Arrays.asList(
                new StringBuilder("a"),
                new StringBuilder("b"),
                new StringBuilder("c")
        );
        SeqProducer<String> producer = SeqProducer.iterableProducer(src)
                .toFluent()
                .peekContextFree(element -> {
                    peeked.add(element.toString());
                    element.append("x");
                })
                .<String>mapContextFree((element, consumer) -> consumer.processElement(element.toString()))
                .unwrap();

        assertEquals(Arrays.asList("ax", "bx", "cx"), collect(producer));
        assertEquals(Arrays.asList("a", "b", "c"), peeked);
    }

    @Test
    public void testPeekContextFreeCancelable() throws Exception {
        List<String> peeked = new ArrayList<>();
        SeqProducer<String> producer = cancelableIterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(peeked::add)
                .unwrap();

        assertContentAndCancellation(Arrays.asList("a", "b", "c"), producer);
    }

    @Test
    public void testWithConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withConsumer((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, result::add);
                })
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testWithConsumerCancelable() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        List<String> result = new ArrayList<>();
        CancelableTask consumeTask = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withConsumer((cancelToken, seqProducer) -> {
                    seqProducer.transferAll(cancelToken, element -> {
                        cancelToken.checkCanceled();
                        result.add(element);
                        if (result.size() == 1) {
                            cancellation.getController().cancel();
                            if (!cancelToken.isCanceled()) {
                                setFirstException(testErrorRef, "Expected cancellation detection.");
                            }
                        }
                    });
                });

        try {
            consumeTask.execute(cancellation.getToken());
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }

        verifyNoException(testErrorRef);
        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    public void testWithContextFreeConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withContextFreeConsumer(result::add)
                .execute(Cancellation.UNCANCELABLE_TOKEN);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testWithContextFreeConsumerCancelable() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        List<String> result = new ArrayList<>();
        CancelableTask consumeTask = cancelableIterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .withContextFreeConsumer(element -> {
                    result.add(element);
                    if (result.size() == 1) {
                        cancellation.getController().cancel();
                    }
                });

        try {
            consumeTask.execute(cancellation.getToken());
            fail("Expected cancellation");
        } catch (OperationCanceledException ex) {
            // Expected
        }

        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    public void testCollect() throws Exception {
        List<String> result = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testCollectCancelable() throws Exception {
        CancellationSource cancellation = Cancellation.createCancellationSource();
        AtomicInteger peekCount = new AtomicInteger();
        FluentSeqProducer<String> producer = SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .peekContextFree(element -> {
                    cancellation.getToken().checkCanceled();
                    if (peekCount.incrementAndGet() == 1) {
                        cancellation.getController().cancel();
                    }
                });

        try {
            producer.collect(cancellation.getToken(), Collectors.toList());
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
            // Expected
        }

        assertEquals(1, peekCount.get());
    }

    @Test
    public void testToSingleGroupProducer() throws Exception {
        List<String> result = new ArrayList<>();
        SeqProducer.iterableProducer(Arrays.asList("a", "b", "c"))
                .toFluent()
                .toSingleGroupProducer()
                .unwrap()
                .transferAllSimple(Cancellation.UNCANCELABLE_TOKEN, result::add);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testLimit0() throws Exception {
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer("a", "b", "c")
                .toFluent()
                .limit(0)
                .unwrap();

        assertEquals(Arrays.asList(), collect(producer));
    }

    @Test
    public void testLimit1() throws Exception {
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer("a", "b", "c")
                .toFluent()
                .limit(1)
                .unwrap();

        assertEquals(Arrays.asList("a"), collect(producer));
    }

    @Test
    public void testLimitSimple() throws Exception {
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer("a", "b", "c")
                .toFluent()
                .limit(2)
                .unwrap();

        assertEquals(Arrays.asList("a", "b"), collect(producer));
    }

    @Test
    public void testLimitExact() throws Exception {
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer("a", "b", "c")
                .toFluent()
                .limit(3)
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }

    @Test
    public void testLimitMore() throws Exception {
        SeqProducer<String> producer = SeqProducer.copiedArrayProducer("a", "b", "c")
                .toFluent()
                .limit(4)
                .unwrap();

        assertEquals(Arrays.asList("a", "b", "c"), collect(producer));
    }
}
