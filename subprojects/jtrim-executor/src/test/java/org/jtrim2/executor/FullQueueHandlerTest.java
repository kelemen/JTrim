package org.jtrim2.executor;

import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FullQueueHandlerTest {
    @Test
    public void testSingletonBlockAlwaysHandler() {
        assertSame(FullQueueHandler.blockAlwaysHandler(), FullQueueHandler.blockAlwaysHandler());
    }

    @Test
    public void testBlockAlwaysHandlerDoesNothing() {
        CancellationToken cancelToken = mock(CancellationToken.class);
        assertNull(FullQueueHandler.blockAlwaysHandler().tryGetFullQueueException(cancelToken));
        verifyZeroInteractions(cancelToken);
    }
}
