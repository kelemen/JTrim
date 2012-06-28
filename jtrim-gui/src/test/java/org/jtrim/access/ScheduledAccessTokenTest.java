/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.access;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class ScheduledAccessTokenTest {

    public ScheduledAccessTokenTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static ScheduledAccessToken<String> createUnblockedToken() {
        return new ScheduledAccessToken<>(
                AccessTokens.createToken("INDEPENDENT"),
                Collections.<AccessToken<String>>emptySet());
    }

    @Test
    public void testNoBlocking() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        final AtomicInteger executed = new AtomicInteger(0);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executed.incrementAndGet();
            }
        }, null);
        assertEquals(1, executed.get());
    }

    @Test
    public void testNoBlockingCleanup() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        final AtomicInteger executed = new AtomicInteger(0);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(),
                new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                assertFalse(canceled);
                assertNull(error);

                executed.incrementAndGet();
            }
        });
        assertEquals(1, executed.get());
    }

    @Test
    public void testReleaseEventSimplePostRelease() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        final AtomicInteger executed = new AtomicInteger(0);
        token.release();
        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                executed.incrementAndGet();
            }
        });
        assertEquals(1, executed.get());
    }

    @Test
    public void testReleaseEventSimplePreRelease() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        final AtomicInteger executed = new AtomicInteger(0);
        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                executed.incrementAndGet();
            }
        });
        token.release();
        assertEquals(1, executed.get());
    }

    @Test
    public void testReleaseEventWithTask() {
        ScheduledAccessToken<String> token = createUnblockedToken();

        final AtomicInteger executed = new AtomicInteger(0);
        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                executed.incrementAndGet();
            }
        });

        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

        token.release();
        assertEquals(1, executed.get());
    }
}
