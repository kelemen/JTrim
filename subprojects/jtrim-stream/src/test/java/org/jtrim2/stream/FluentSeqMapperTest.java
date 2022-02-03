package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

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
    public void testApply() throws Exception {
        SeqMapper<String, String> originalMapper = simpleTestMapper(e -> e + "x");
        SeqMapper<String, String> mapper = originalMapper
                .toFluent()
                .<String, String>apply(wrapped -> {
                    assertSame(originalMapper, wrapped);
                    return simpleTestMapper(e -> e + "y");
                })
                .unwrap();

        List<String> expected = Arrays.asList("ay", "by", "cy", "dy", "ey", "fy");
        assertEquals(expected, collect(testSrc(), mapper));
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
}
