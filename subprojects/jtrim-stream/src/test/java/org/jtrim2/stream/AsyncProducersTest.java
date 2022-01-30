package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.RepeatTest;
import org.jtrim2.testutils.RepeatTestRule;
import org.jtrim2.testutils.TestUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class AsyncProducersTest {
    @Rule
    public final RepeatTestRule repeatRule = new RepeatTestRule();

    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(AsyncProducers.class);
    }

    private void testNormal(Supplier<AsyncProducerRef<String>> factory) {
        AsyncProducerRef<String> producerRef = factory.get();

        AsyncElementSink<String> sink = producerRef.getElementSink();
        SeqProducer<String> producer = producerRef.getProducerFactory().get();

        assertTrue("producer.type", producer instanceof AsyncSourceProducer);
        assertTrue("sink.type", sink instanceof DefaultAsyncElementSource);

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> received = new ArrayList<>();

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(TestUtils.toSafeRunnable(() -> {
            producer.transferAll(Cancellation.UNCANCELABLE_TOKEN, received::add);
        }));
        tasks.add(TestUtils.toSafeRunnable(() -> {
            try {
                for (String element: expected) {
                    assertTrue("put", sink.tryPut(Cancellation.UNCANCELABLE_TOKEN, element));
                }
            } finally {
                sink.finish(null);
            }
        }));

        Tasks.runConcurrently(tasks);

        assertEquals(expected, received);
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testNormalUnlimited() {
        testNormal(() -> AsyncProducers.createAsyncSourcedProducer(10));
    }

    @Test(timeout = 20000)
    @RepeatTest(20)
    public void testNormalLimited() {
        testNormal(() -> AsyncProducers.createAsyncSourcedProducer(1, 1));
    }
}
