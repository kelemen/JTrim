/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.access;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
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

    /**
     * Test of createExecutor method, of class ScheduledAccessToken.
     */
    @Test
    public void testNoBlocking() {
        ScheduledAccessToken<String> token = new ScheduledAccessToken<>(
                AccessTokens.createToken("INDEPENDENT"),
                Collections.<AccessToken<String>>emptySet());

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
}
