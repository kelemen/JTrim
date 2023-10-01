package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.testutils.executor.TestThreadFactory;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.jtrim2.stream.SeqMapperTest.*;
import static org.junit.Assert.*;

public class FluentSeqMapperTest {
    private static SeqProducer<String> testSrc() {
        return SeqProducer.copiedArrayProducer("a", "b", "c", "d", "e", "f");
    }

    static <T, R> SeqMapper<T, R> simpleTestMapper(Function<? super T, ? extends R> mapper) {
        return SeqMapper.fromElementMapper(ElementMapper.oneToOneMapper(mapper));
    }

    @Test
    public void testApply() {
        testApply(FluentSeqMapper::apply);
    }

    @Test
    public void testApplyFluent() {
        testApply((src, configurer) -> src.applyFluent(srcWrapped -> configurer.apply(srcWrapped.unwrap()).toFluent()));
    }

    private void testApply(
            BiFunction<
                    FluentSeqMapper<String, Integer>,
                    Function<SeqMapper<String, Integer>, SeqMapper<Double, Long>>,
                    FluentSeqMapper<Double, Long>
                    > applier
    ) {
        SeqMapper<String, Integer> srcMapper = (cancelToken, seqProducer, seqConsumer) -> {
            System.out.println("testApply.a");
        };
        SeqMapper<Double, Long> newMapper = (cancelToken, seqProducer, seqConsumer) -> {
            System.out.println("testApply.b");
        };

        assertSame(newMapper, applier
                .apply(srcMapper.toFluent(), wrapped -> {
                    assertSame(srcMapper, wrapped);
                    return newMapper;
                })
                .unwrap()
        );
    }

    @Test
    public void testMapContextFreeIdentityDirectly() throws Exception {
        ElementMapper<String, String> mapper = ElementMappers.identityMapper();
        List<String> result = new ArrayList<>();
        mapper.map("a", result::add);
        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    public void testMapIdentity() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.identity();

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMapIdentityConcat() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.<String>identity()
                .toFluent()
                .map(SeqMapper.<String>identity().toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMapIdentity1() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.<String>identity()
                .toFluent()
                .map(simpleTestMapper((String e) -> e + "x").toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMapIdentity2() throws Exception {
        SeqMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .map(SeqMapper.<String>identity().toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMap() throws Exception {
        SeqMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .map(simpleTestMapper(e -> e + "y").toFluent())
                .unwrap();

        List<String> expected = Arrays.asList("axy", "bxy", "cxy", "dxy", "exy", "fxy");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMapContextFreeIdentity() throws Exception {
        SeqMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .mapContextFree(ElementMapper.identity())
                .unwrap();

        List<String> expected = Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testMapContextFree() throws Exception {
        SeqMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .mapContextFree(ElementMapper.oneToOneMapper(e -> e + "y"))
                .unwrap();

        List<String> expected = Arrays.asList("axy", "bxy", "cxy", "dxy", "exy", "fxy");
        assertEquals(expected, collect(testSrc(), mapper));
    }

    @Test
    public void testToSingleShotGroupMapper() throws Exception {
        FluentSeqGroupMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .toSingleShotGroupMapper();

        List<String> result = testSrc()
                .toFluent()
                .toSingleGroupProducer()
                .mapGroups(mapper)
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());

        assertEquals(Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx"), result);
    }

    @Test
    public void testToSingleShotGroupMapperMultiCall() throws Exception {
        FluentSeqGroupMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .toSingleShotGroupMapper();

        SeqGroupProducer<String> src = FluentSeqGroupProducerTest.iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );

        List<String> peeked = new ArrayList<>();
        FluentSeqGroupProducer<String> producer = src
                .toFluent()
                .mapGroups(mapper)
                .peekContextFree(peeked::add);

        try {
            producer.collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());
            fail("Expected failure.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("multiple"));
        }

        assertEquals(Arrays.asList("ax", "bx", "cx"), peeked);
    }

    @Test
    public void testToContextFreeGroupMapper() throws Exception {
        FluentSeqGroupMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .toContextFreeGroupMapper();

        List<String> result = testSrc()
                .toFluent()
                .toSingleGroupProducer()
                .mapGroups(mapper)
                .collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());

        assertEquals(Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx"), result);
    }

    @Test
    public void testToContextFreeGroupMapperMultiCall() throws Exception {
        FluentSeqGroupMapper<String, String> mapper = simpleTestMapper((String e) -> e + "x")
                .toFluent()
                .toContextFreeGroupMapper();

        SeqGroupProducer<String> src = FluentSeqGroupProducerTest.iterableProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e")
        );

        FluentSeqGroupProducer<String> producer = src
                .toFluent()
                .mapGroups(mapper);

        List<String> result = producer.collect(Cancellation.UNCANCELABLE_TOKEN, Collectors.toList());
        assertEquals(Arrays.asList("ax", "bx", "cx", "dx", "ex"), result);
    }

    private void testInBackground(
            Function<FluentSeqMapper<String, String>, FluentSeqMapper<String, String>> inBackground,
            Consumer<? super String> peekAction
    ) throws Exception {
        SeqMapper<String, String> src = SeqMapper.oneToOneMapper((String e) -> e + "y");

        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        AtomicInteger peekCount = new AtomicInteger(0);
        SeqMapper<String, String> mapper = inBackground
                .apply(src.toFluent())
                .mapContextFree(ElementMapper.oneToOneMapper(element -> {
                    peekCount.incrementAndGet();
                    try {
                        peekAction.accept(element);
                    } catch (Throwable ex) {
                        setFirstException(testErrorRef, "peek error", ex);
                    }
                    return element;
                }))
                .unwrap();

        assertEquals(
                Arrays.asList("ay", "by", "cy", "dy", "ey", "fy"),
                collect(testSrc(), mapper)
        );

        assertEquals(6, peekCount.get());
        verifyNoException(testErrorRef);
    }

    @Test(timeout = 10000)
    public void testInBackgroundOwned() throws Exception {
        String executorName = "Test-Executor-testInBackgroundOwned";
        testInBackground(
                mapper -> mapper.inBackground(executorName, 0),
                element -> {
                    String threadName = Thread.currentThread().getName();
                    if (!threadName.contains(executorName)) {
                        throw new IllegalStateException("Expected to run in background, but running in " + threadName);
                    }
                }
        );
    }

    @Test(timeout = 10000)
    public void testInBackgroundThreadFactory() throws Exception {
        var threadFactory = new TestThreadFactory("Test-Executor-testInBackgroundThreadFactory");
        testInBackground(
                mapper -> mapper.inBackground(threadFactory, 0),
                element -> {
                    if (!threadFactory.isExecutingInThis()) {
                        String threadName = Thread.currentThread().getName();
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
                    mapper -> mapper.inBackground(executor, 0),
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
