package org.jtrim.access.query;

import java.util.concurrent.*;
import org.jtrim.access.AccessToken;
import org.jtrim.access.AccessTokens;
import org.jtrim.concurrent.ExecutorConverter;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class AutoReportRewQueryExecutorTest {
    private static final int PATIENCE_IN_SECONDS = 5;
    private static final int CHECK_EXECUTE_TEST_COUNT = 20;
    private final TaskExecutorService[] executors;

    public AutoReportRewQueryExecutorTest() {
        executors = new TaskExecutorService[2];
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        for (int i = 0; i < executors.length; i++) {
            executors[i] = new ThreadPoolTaskExecutor("AutoReportRewQueryExecutorTest-pool", 1);
        }
    }

    @After
    public void tearDown() {
        for (int i = 0; i < executors.length; i++) {
            if (executors[i] != null) {
                executors[i].shutdown();
                executors[i] = null;
            }
        }
    }

    private static AutoReportRewQueryExecutor createDefaultInstance() {
        return new AutoReportRewQueryExecutor(1, TimeUnit.MINUTES);
    }

    private void assertWillTerminate(AccessToken<?> token) throws InterruptedException {
        assertTrue(token.getAccessID().toString() + " did not terminate within a reasonable time.",
                token.awaitTermination(PATIENCE_IN_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * @deprecated Replace with TaskExecutorService.
     */
    @Deprecated
    private ExecutorService asExecutorService(int index) {
        return ExecutorConverter.asExecutorService(executors[index]);
    }

    private void checkExecute(int stepCount, int delayMS)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String arg = "arg";
        RewQueryExecutor rewExecutor = createDefaultInstance();
        TestRewQuery query = new TestRewQuery(executors[0], arg, stepCount, delayMS);

        AccessToken<?> readToken = AccessTokens.createToken("READ-TOKEN", asExecutorService(1));
        AccessToken<?> writeToken = AccessTokens.createToken("WRITE-TOKEN", asExecutorService(1));

        Future<?> future = rewExecutor.execute(query,
                readToken,
                writeToken);
        future.get(PATIENCE_IN_SECONDS, TimeUnit.SECONDS);

        String[] outputs = query.getWrittenOutput();

        if (stepCount > 0) {
            assertTrue(outputs.length > 0);
            assertEquals(TestRewQuery.getExpectedResult(arg, stepCount - 1), outputs[outputs.length - 1]);
        }
        else {
            assertTrue(outputs.length == 0);
        }

        assertFalse(readToken.isShutdown());
        assertFalse(writeToken.isShutdown());
    }

    private void checkExecuteNow(int stepCount, int delayMS)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String arg = "arg";
        RewQueryExecutor rewExecutor = createDefaultInstance();
        TestRewQuery query = new TestRewQuery(executors[0], arg, stepCount, delayMS);

        AccessToken<?> readToken = AccessTokens.createToken("READ-TOKEN", asExecutorService(1));
        AccessToken<?> writeToken = AccessTokens.createToken("WRITE-TOKEN", asExecutorService(1));

        Future<?> future = rewExecutor.executeNow(query,
                readToken,
                writeToken);
        future.get(PATIENCE_IN_SECONDS, TimeUnit.SECONDS);

        String[] outputs = query.getWrittenOutput();

        if (stepCount > 0) {
            assertTrue(outputs.length > 0);
            assertEquals(TestRewQuery.getExpectedResult(arg, stepCount - 1), outputs[outputs.length - 1]);
        }
        else {
            assertTrue(outputs.length == 0);
        }

        assertFalse(readToken.isShutdown());
        assertFalse(writeToken.isShutdown());
    }

    private void checkExecuteAndRelease(int stepCount, int delayMS)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String arg = "arg";
        RewQueryExecutor rewExecutor = createDefaultInstance();
        TestRewQuery query = new TestRewQuery(executors[0], arg, stepCount, delayMS);

        AccessToken<?> readToken = AccessTokens.createToken("READ-TOKEN", asExecutorService(1));
        AccessToken<?> writeToken = AccessTokens.createToken("WRITE-TOKEN", asExecutorService(1));

        Future<?> future = rewExecutor.executeAndRelease(query,
                readToken,
                writeToken);
        future.get(PATIENCE_IN_SECONDS, TimeUnit.SECONDS);

        String[] outputs = query.getWrittenOutput();

        if (stepCount > 0) {
            assertTrue(outputs.length > 0);
            assertEquals(TestRewQuery.getExpectedResult(arg, stepCount - 1), outputs[outputs.length - 1]);
        }
        else {
            assertTrue(outputs.length == 0);
        }

        assertWillTerminate(readToken);
        assertWillTerminate(writeToken);
    }

     private void checkExecuteNowAndRelease(int stepCount, int delayMS)
             throws InterruptedException, ExecutionException, TimeoutException {
        final String arg = "arg";
        RewQueryExecutor rewExecutor = createDefaultInstance();
        TestRewQuery query = new TestRewQuery(executors[0], arg, stepCount, delayMS);

        AccessToken<?> readToken = AccessTokens.createToken("READ-TOKEN", asExecutorService(1));
        AccessToken<?> writeToken = AccessTokens.createToken("WRITE-TOKEN", asExecutorService(1));

        Future<?> future = rewExecutor.executeNowAndRelease(query,
                readToken,
                writeToken);
        future.get(PATIENCE_IN_SECONDS, TimeUnit.SECONDS);

        String[] outputs = query.getWrittenOutput();

        if (stepCount > 0) {
            assertTrue(outputs.length > 0);
            assertEquals(TestRewQuery.getExpectedResult(arg, stepCount - 1), outputs[outputs.length - 1]);
        }
        else {
            assertTrue(outputs.length == 0);
        }

        assertWillTerminate(readToken);
        assertWillTerminate(writeToken);
    }

    /**
     * Test of execute method, of class AutoReportRewQueryExecutor.
     */
    @Test
    public void testExecute() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < CHECK_EXECUTE_TEST_COUNT; i++) {
            checkExecute(5, 0);
        }
    }

    /**
     * Test of executeNow method, of class AutoReportRewQueryExecutor.
     */
    @Test
    public void testExecuteNow() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < CHECK_EXECUTE_TEST_COUNT; i++) {
            checkExecuteNow(5, 0);
        }
    }

    /**
     * Test of executeAndRelease method, of class AutoReportRewQueryExecutor.
     */
    @Test
    public void testExecuteAndRelease() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < CHECK_EXECUTE_TEST_COUNT; i++) {
            checkExecuteAndRelease(5, 0);
        }
    }

    /**
     * Test of executeNowAndRelease method, of class AutoReportRewQueryExecutor.
     */
    @Test
    public void testExecuteNowAndRelease() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < CHECK_EXECUTE_TEST_COUNT; i++) {
            checkExecuteNowAndRelease(5, 0);
        }
    }

    /**
     * Test of toString method, of class AutoReportRewQueryExecutor.
     */
    @Test
    public void testToString() {
        String str = new AutoReportRewQueryExecutor(1, TimeUnit.MINUTES).toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }
}
