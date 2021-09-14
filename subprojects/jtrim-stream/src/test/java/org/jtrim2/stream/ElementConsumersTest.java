package org.jtrim2.stream;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ElementConsumersTest {
    @Test
    public void testNoOpConsumerUnique() {
        assertSame(ElementConsumers.noOpConsumer(), ElementConsumers.noOpConsumer());
        assertSame(ElementConsumers.noOpConsumer(), ElementConsumer.noOp());
    }

    @Test
    public void testNoOpConsumerNoFailure() throws Exception {
        ElementConsumers.noOpConsumer().processElement("test-job");
    }

    @Test
    public void testDrainingSerialConsumerUnique() {
        assertSame(ElementConsumers.drainingSeqConsumer(), ElementConsumers.drainingSeqConsumer());
        assertSame(ElementConsumers.drainingSeqConsumer(), SeqConsumer.draining());
    }

    @Test
    public void testDrainingConsumerDefUnique() {
        assertSame(ElementConsumers.drainingSeqGroupConsumer(), ElementConsumers.drainingSeqGroupConsumer());
        assertSame(ElementConsumers.drainingSeqGroupConsumer(), SeqGroupConsumer.draining());
    }

    @Test
    public void testDrainingConsumer() throws Exception {
        CancellationToken cancelToken = mock(CancellationToken.class);
        Runnable called = mock(Runnable.class);

        ElementConsumers.drainingSeqGroupConsumer().consumeAll(cancelToken, (receivedCancelToken, serialConsumer) -> {
            assertSame(cancelToken, receivedCancelToken);
            assertSame(ElementConsumers.drainingSeqConsumer(), serialConsumer);
            called.run();
        });

        verifyZeroInteractions(cancelToken);
        verify(called).run();
    }

    @Test
    public void testDrainingConsumerExceptionPropagation() throws Exception {
        TestException expectedException = new TestException();
        try {
            ElementConsumers
                    .drainingSeqGroupConsumer()
                    .consumeAll(
                            Cancellation.UNCANCELABLE_TOKEN,
                            (cancelToken, consumer) -> {
                                throw expectedException;
                            }
                    );
            fail("Expected TestException");
        } catch (TestException ex) {
            assertSame(expectedException, ex);
        }

    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
