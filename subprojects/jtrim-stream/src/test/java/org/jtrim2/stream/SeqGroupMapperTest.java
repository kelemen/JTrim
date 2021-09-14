package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeqGroupMapperTest {
    public static <T, R> List<List<R>> collect(
            SeqGroupProducer<? extends T> src,
            SeqGroupMapper<? super T, ? extends R> mapper) throws Exception {

        List<List<R>> result = Collections.synchronizedList(new ArrayList<>());
        mapper.mapAll(Cancellation.UNCANCELABLE_TOKEN, src, (cancelToken, seqGroupProducer) -> {
            seqGroupProducer.transferAll(cancelToken, (cancelToken2, seqProducer) -> {
                List<R> groupResult = new ArrayList<>();
                result.add(groupResult);
                seqProducer.transferAll(cancelToken2, groupResult::add);
            });
        });
        return result;
    }

    private static <T, R> List<R> mapOne(SeqGroupMapper<? super T, ? extends R> mapper, T value) throws Exception {
        List<R> result = new ArrayList<>(1);
        mapper.mapAll(
                Cancellation.UNCANCELABLE_TOKEN,
                SeqProducer.copiedArrayProducer(value).toFluent().toSingleGroupProducer().unwrap(),
                (cancelToken, seqGroupProducer) -> {
                    seqGroupProducer.transferAll(cancelToken, (cancelToken2, seqProducer) -> {
                        seqProducer.transferAll(cancelToken2, result::add);
                    });
                }
        );
        return result;
    }

    private static <T, R> void verifyMapToSame(
            SeqGroupMapper<? super T, ? extends R> mapper,
            T value) throws Exception {

        assertEquals(Arrays.asList(value), mapOne(mapper, value));
    }

    @Test
    public void testIdentitySingleton() {
        assertSame(SeqMapper.identity(), SeqMapper.identity());
    }

    @Test
    public void testIdentity() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.identity();
        for (String v: Arrays.asList("", "a", "xyz")) {
            verifyMapToSame(mapper, v);
        }
    }

    private static SeqGroupProducer<List<String>> testBatchedSrc() {
        List<List<String>> group1 = Arrays.asList(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );
        List<List<String>> group2 = Arrays.asList(
                Arrays.asList("g"),
                Arrays.asList("h", "i"),
                Arrays.asList("j", "k", "l")
        );
        List<List<String>> group3 = Collections.emptyList();

        return FluentSeqGroupProducerTest.iterableProducer(group1, group2, group3);
    }

    @Test
    public void testFlatteningMapperSingleton() {
        assertSame(SeqGroupMapper.flatteningMapper(), SeqGroupMapper.flatteningMapper());
    }

    @Test
    public void testFlatteningMapper() throws Exception {
        List<String> expected1 = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> expected2 = Arrays.asList("g", "h", "i", "j", "k", "l");
        List<String> expected3 = Arrays.asList();
        assertEquals(
                Arrays.asList(expected1, expected2, expected3),
                collect(testBatchedSrc(), SeqGroupMapper.flatteningMapper())
        );
    }

    @Test
    public void testFromElementMapper() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper
                .fromElementMapper(ElementMapper.oneToOneMapper(v -> v + "x"));

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }

    @Test
    public void testFromMapper() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.fromMapper(SeqMapper.oneToOneMapper(v -> v + "x"));

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }

    @Test
    public void testOneToOneMapper() throws Exception {
        SeqGroupMapper<String, String> mapper = SeqGroupMapper.oneToOneMapper(v -> v + "x");

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }
}
