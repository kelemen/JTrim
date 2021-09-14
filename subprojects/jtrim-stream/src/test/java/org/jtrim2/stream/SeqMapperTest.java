package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeqMapperTest {
    public static <T, R> List<R> collect(
            SeqProducer<? extends T> src,
            SeqMapper<? super T, ? extends R> mapper) throws Exception {

        List<R> result = Collections.synchronizedList(new ArrayList<>());
        mapper.mapAll(Cancellation.UNCANCELABLE_TOKEN, src, (cancelToken, seqProducer) -> {
            seqProducer.transferAll(cancelToken, result::add);
        });
        return result;
    }

    private static <T, R> List<R> mapOne(SeqMapper<? super T, ? extends R> mapper, T value) throws Exception {
        List<R> result = new ArrayList<>(1);
        mapper.mapAll(
                Cancellation.UNCANCELABLE_TOKEN,
                SeqProducer.copiedArrayProducer(value),
                (cancelToken, seqProducer) -> seqProducer.transferAll(cancelToken, result::add)
        );
        return result;
    }

    private static <T, R> void verifyMapToSame(
            SeqMapper<? super T, ? extends R> mapper,
            T value) throws Exception {

        assertEquals(Arrays.asList(value), mapOne(mapper, value));
    }

    @Test
    public void testIdentitySingleton() {
        assertSame(SeqMapper.identity(), SeqMapper.identity());
    }

    @Test
    public void testIdentity() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.identity();
        for (String v: Arrays.asList("", "a", "xyz")) {
            verifyMapToSame(mapper, v);
        }
    }

    private static SeqProducer<List<String>> testBatchedSrc() {
        return SeqProducer.copiedArrayProducer(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("d", "e"),
                Arrays.asList("f")
        );
    }

    @Test
    public void testFlatteningMapperSingleton() {
        assertSame(SeqMapper.flatteningMapper(), SeqMapper.flatteningMapper());
    }

    @Test
    public void testFlatteningMapper() throws Exception {
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(expected, collect(testBatchedSrc(), SeqMapper.flatteningMapper()));
    }

    @Test
    public void testFromElementMapper() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.fromElementMapper(ElementMapper.oneToOneMapper(v -> v + "x"));

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }

    @Test
    public void testOneToOneMapper() throws Exception {
        SeqMapper<String, String> mapper = SeqMapper.oneToOneMapper(v -> v + "x");

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }
}
