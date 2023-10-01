package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.executor.TestThreadFactory;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.jtrim2.stream.SeqGroupMapperTest.*;
import static org.junit.Assert.*;

public class FluentSeqGroupMapperTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(ElementMappers.class);
    }

    private static SeqGroupProducer<String> testSrc() {
        List<String> group1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> group2 = Arrays.asList("g", "h", "i", "j", "k", "l");
        List<String> group3 = Arrays.asList();

        return FluentSeqGroupProducerTest.iterableProducer(group1, group2, group3);
    }

    @Test
    public void testApply() {
        testApply(FluentSeqGroupMapper::apply);
    }

    @Test
    public void testApplyFluent() {
        testApply((src, configurer) -> src.applyFluent(srcWrapped -> configurer.apply(srcWrapped.unwrap()).toFluent()));
    }

    private void testApply(
            BiFunction<
                    FluentSeqGroupMapper<String, Integer>,
                    Function<SeqGroupMapper<String, Integer>, SeqGroupMapper<Double, Long>>,
                    FluentSeqGroupMapper<Double, Long>
                    > applier
    ) {
        SeqGroupMapper<String, Integer> srcMapper = (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            System.out.println("testApply.a");
        };
        SeqGroupMapper<Double, Long> newMapper = (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
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
    public void testMapGroupsIdentity() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.identity();

        List<String> expected1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> expected2 = Arrays.asList("g", "h", "i", "j", "k", "l");
        List<String> expected3 = Arrays.asList();

        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapGroupsIdentityConcat() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.<String>identity()
                .toFluent()
                .mapGroups(SeqGroupMapper.<String>identity().toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> expected2 = Arrays.asList("g", "h", "i", "j", "k", "l");
        List<String> expected3 = Arrays.asList();

        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapGroupsIdentity1() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.<String>identity()
                .toFluent()
                .mapGroups(SeqGroupMapper.oneToOneMapper((String e) -> e + "y").toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy");
        List<String> expected2 = Arrays.asList("gy", "hy", "iy", "jy", "ky", "ly");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapGroupsIdentity2() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "y")
                .toFluent()
                .mapGroups(SeqGroupMapper.<String>identity().toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy");
        List<String> expected2 = Arrays.asList("gy", "hy", "iy", "jy", "ky", "ly");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapGroups() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "x")
                .toFluent()
                .mapGroups(SeqGroupMapper.oneToOneMapper(e -> e + "y").toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("axy", "bxy", "cxy", "dxy", "exy", "fxy");
        List<String> expected2 = Arrays.asList("gxy", "hxy", "ixy", "jxy", "kxy", "lxy");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapIdentity() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "y")
                .toFluent()
                .map(SeqMapper.<String>identity().toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy");
        List<String> expected2 = Arrays.asList("gy", "hy", "iy", "jy", "ky", "ly");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMap() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "x")
                .toFluent()
                .map(FluentSeqMapperTest.simpleTestMapper(e -> e + "y").toFluent())
                .unwrap();

        List<String> expected1 = Arrays.asList("axy", "bxy", "cxy", "dxy", "exy", "fxy");
        List<String> expected2 = Arrays.asList("gxy", "hxy", "ixy", "jxy", "kxy", "lxy");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapContextFreeIdentity() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "y")
                .toFluent()
                .mapContextFree(ElementMapper.identity())
                .unwrap();

        List<String> expected1 = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy");
        List<String> expected2 = Arrays.asList("gy", "hy", "iy", "jy", "ky", "ly");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    @Test
    public void testMapContextFree() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper((String e) -> e + "x")
                .toFluent()
                .mapContextFree(ElementMapper.oneToOneMapper(e -> e + "y"))
                .unwrap();

        List<String> expected1 = Arrays.asList("axy", "bxy", "cxy", "dxy", "exy", "fxy");
        List<String> expected2 = Arrays.asList("gxy", "hxy", "ixy", "jxy", "kxy", "lxy");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testSrc(), mapper)
        );
    }

    private void testInBackground(
            Function<FluentSeqGroupMapper<String, String>, FluentSeqGroupMapper<String, String>> inBackground,
            Consumer<? super String> peekAction) throws Exception {

        SeqGroupMapper<String, String> src = SeqGroupMapper.oneToOneMapper((String e) -> e + "y");

        AtomicReference<RuntimeException> testErrorRef = new AtomicReference<>();
        AtomicInteger peekCount = new AtomicInteger(0);
        SeqGroupMapper<String, String> mapper = inBackground
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

        List<String> expected = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy", "gy", "hy", "iy", "jy", "ky", "ly");
        assertEquals(
                Arrays.asList(expected),
                collect(testSrc(), mapper)
        );

        assertEquals(12, peekCount.get());
        verifyNoException(testErrorRef);
    }

    @Test(timeout = 10000)
    public void testInBackgroundOwned() throws Exception {
        String executorName = "Test-Executor-testInBackgroundOwned";
        testInBackground(
                mapper -> mapper.inBackground(executorName, 1, 0),
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
                mapper -> mapper.inBackground(threadFactory, 1, 0),
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
                    mapper -> mapper.inBackground(executor, 1, 0),
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

    private static SeqGroupMapper<String, String> collectingMapper(List<List<String>> result) {
        return (cancelToken, seqGroupProducer, seqGroupConsumer) -> {
            seqGroupConsumer.consumeAll(cancelToken, (cancelToken2, seqConsumer) -> {
                seqGroupProducer.transferAll(cancelToken2, (cancelToken3, seqProducer) -> {
                    List<String> groupResult = new ArrayList<>();
                    result.add(groupResult);
                    seqConsumer.consumeAll(cancelToken3, (cancelToken4, consumer) -> {
                        seqProducer.transferAll(cancelToken4, e -> {
                            consumer.processElement(e + "x");
                            groupResult.add(e);
                        });
                    });
                });
            });
        };
    }

    @Test
    public void testToDrainingConsumerIdentity() {
        SeqGroupMapper<String, String> baseMapper = SeqGroupMapper.identity();
        SeqGroupConsumer<String> consumer = baseMapper.toFluent()
                .toDrainingConsumer()
                .unwrap();
        assertSame(SeqGroupConsumer.draining(), consumer);
    }

    @Test
    public void testToDrainingConsumer() throws Exception {
        List<List<String>> result = new ArrayList<>();
        SeqGroupMapper<String, String> baseMapper = collectingMapper(result);

        SeqGroupConsumer<String> consumer = baseMapper
                .toFluent()
                .toDrainingConsumer()
                .unwrap();

        assertEquals(Collections.emptyList(), result);
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());
        List<String> expected1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> expected2 = Arrays.asList("g", "h", "i", "j", "k", "l");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                result
        );
    }

    @Test
    public void testToConsumerIdentity() throws Exception {
        List<List<String>> result = new ArrayList<>();
        SeqGroupMapper<String, String> baseMapper = SeqGroupMapper.identity();

        SeqGroupConsumer<String> consumer = baseMapper
                .toFluent()
                .toConsumer(SeqGroupConsumerTest.collectingConsumer(result, e -> e + "y").toFluent())
                .unwrap();

        assertEquals(Collections.emptyList(), result);
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());
        List<List<String>> expected = Arrays.asList(
                Arrays.asList("ay", "by", "cy", "dy", "ey", "fy"),
                Arrays.asList("gy", "hy", "iy", "jy", "ky", "ly"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testToConsumerToNoOp() throws Exception {
        List<List<String>> result = new ArrayList<>();
        SeqGroupMapper<String, String> baseMapper = collectingMapper(result);

        SeqGroupConsumer<String> consumer = baseMapper
                .toFluent()
                .toConsumer(SeqGroupConsumer.draining().toFluent())
                .unwrap();

        assertEquals(Collections.emptyList(), result);
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());
        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c", "d", "e", "f"),
                Arrays.asList("g", "h", "i", "j", "k", "l"),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }

    @Test
    public void testToConsumerIdentityToNoOp() {
        SeqGroupMapper<String, String> baseMapper = SeqGroupMapper.identity();

        SeqGroupConsumer<String> consumer = baseMapper
                .toFluent()
                .toConsumer(SeqGroupConsumer.draining().toFluent())
                .unwrap();

        assertSame(SeqGroupConsumer.draining(), consumer);
    }

    @Test
    public void testToConsumer() throws Exception {
        List<List<String>> result = new ArrayList<>();
        SeqGroupMapper<String, String> baseMapper = collectingMapper(result);

        SeqGroupConsumer<String> consumer = baseMapper
                .toFluent()
                .toConsumer(SeqGroupConsumerTest.collectingConsumer(result, Function.identity()).toFluent())
                .unwrap();

        assertEquals(Collections.emptyList(), result);
        consumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, testSrc());
        List<List<String>> expected = Arrays.asList(
                Arrays.asList("a", "b", "c", "d", "e", "f"),
                Arrays.asList("ax", "bx", "cx", "dx", "ex", "fx"),
                Arrays.asList("g", "h", "i", "j", "k", "l"),
                Arrays.asList("gx", "hx", "ix", "jx", "kx", "lx"),
                Arrays.asList(),
                Arrays.asList()
        );
        assertEquals(expected, result);
    }
}
