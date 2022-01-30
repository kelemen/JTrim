package org.jtrim2.stream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;

public class AsyncSourceProducerTest {
    @Test
    public void testNormalTransfer() throws Exception {
        List<String> expected = Collections.unmodifiableList(Arrays.asList("a", "b", "c"));
        TestSource source = new TestSource(expected);
        SeqProducer<String> producer = new AsyncSourceProducer<>(source);

        List<String> received = new ArrayList<>();
        producer.transferAll(Cancellation.UNCANCELABLE_TOKEN, received::add);

        assertEquals("received", expected, received);
        source.assertFinished(null);
    }

    @Test
    public void testFailureInTransfer() {
        Exception expectedFailure = new Exception("testFailureInTransfer");

        List<String> expected = Collections.unmodifiableList(Arrays.asList("a", "b", "c"));
        TestSource source = new TestSource(expected, expectedFailure);
        SeqProducer<String> producer = new AsyncSourceProducer<>(source);

        List<String> received = new ArrayList<>();

        try {
            producer.transferAll(Cancellation.UNCANCELABLE_TOKEN, received::add);
            fail("Expected failure");
        } catch (Exception ex) {
            assertSame("transferAll:exception", expectedFailure, ex);
        }

        assertEquals("received", expected, received);
        source.assertFinished(expectedFailure);
    }

    private static final class TestSource implements PollableElementSource<String> {
        private final Deque<String> queue;
        private final AtomicReference<Throwable> finishError;
        private final AtomicInteger finishCount;
        private final AtomicReference<RuntimeException> unexpectedEvent;
        private final Exception finalFailure;

        public TestSource(Collection<? extends String> elements) {
            this(elements, null);
        }

        public TestSource(Collection<? extends String> elements, Exception finalFailure) {
            this.queue = new ArrayDeque<>(elements);
            this.finishError = new AtomicReference<>();
            this.finishCount = new AtomicInteger(0);
            this.unexpectedEvent = new AtomicReference<>();
            this.finalFailure = finalFailure;
        }

        @Override
        public String getNext(CancellationToken cancelToken) throws Exception {
            if (finishCount.get() > 0) {
                setFirstException(unexpectedEvent, "getNext() after finish");
            }

            String result = queue.pollFirst();
            if (result == null && finalFailure != null) {
                throw finalFailure;
            }
            return result;
        }

        @Override
        public void finish(Throwable error) {
            finishCount.getAndIncrement();
            finishError.set(error);
        }

        public void assertFinished(Throwable expected) {
            verifyNoException(unexpectedEvent);

            assertEquals("finish", 1, finishCount.get());
            assertSame("error", expected, finishError.get());
        }
    }
}
