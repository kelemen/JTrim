package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElementMapperTest {
    @Test
    public void testIdentitySingleton() {
        assertSame(ElementMapper.identity(), ElementMapper.identity());
    }

    private static <T, R> List<R> mapOne(ElementMapper<? super T, ? extends R> mapper, T value) throws Exception {
        List<R> result = new ArrayList<>(1);
        mapper.map(value, result::add);
        return result;
    }

    private static <T, R> void verifyMapToSame(
            ElementMapper<? super T, ? extends R> mapper,
            T value) throws Exception {

        assertEquals(Arrays.asList(value), mapOne(mapper, value));
    }

    @Test
    public void testIdentity() throws Exception {
        ElementMapper<String, String> mapper = ElementMapper.identity();
        for (String v: Arrays.asList("", "a", "xyz")) {
            verifyMapToSame(mapper, v);
        }
    }

    @Test
    public void testFlatteningMapperSingleton() {
        assertSame(ElementMapper.flatteningMapper(), ElementMapper.flatteningMapper());
    }

    @Test
    public void testFlatteningMapper() throws Exception {
        ElementMapper<Iterable<String>, String> mapper = ElementMapper.flatteningMapper();
        List<String> result = new ArrayList<>();
        mapper.map(Arrays.asList("a", "b", "c"), result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testFlatteningMapperWithDNoOp() throws Exception {
        // We can't really test much more than that there is no failure.
        ElementMapper<Iterable<String>, String> mapper = ElementMapper.flatteningMapper();
        mapper.map(Arrays.asList("a", "b", "c"), ElementConsumers.noOpConsumer());
    }

    @Test
    public void testFilteringMapper() throws Exception {
        ElementMapper<String, String> mapper = ElementMapper.filteringMapper(v -> v.contains("x"));

        verifyMapToSame(mapper, "vx");
        verifyMapToSame(mapper, "x");

        assertEquals(Collections.emptyList(), mapOne(mapper, "a"));
        assertEquals(Collections.emptyList(), mapOne(mapper, ""));
    }

    @Test
    public void testOneToOneMapper() throws Exception {
        ElementMapper<String, String> mapper = ElementMapper.oneToOneMapper(v -> v + "x");

        assertEquals(Arrays.asList("x"), mapOne(mapper, ""));
        assertEquals(Arrays.asList("ax"), mapOne(mapper, "a"));
    }
}
