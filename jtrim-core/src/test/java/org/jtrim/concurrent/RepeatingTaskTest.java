package org.jtrim.concurrent;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class RepeatingTaskTest {

    public RepeatingTaskTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of execute method, of class RepeatingTask.
     */
    @Test
    public void testExecute() {
    }

    private void testRun(int runCount, final boolean exceptionOnReRun, boolean startImmediately) throws InterruptedException {
        final Runnable task = mock(Runnable.class);
        final CountDownLatch runLatch = new CountDownLatch(runCount);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            final Callable<Boolean> runAndTestMethod = new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    task.run();
                    runLatch.countDown();
                    boolean result = runLatch.getCount() > 0;
                    if (result && exceptionOnReRun) {
                        throw new RuntimeException();
                    }

                    return result;
                }
            };

            RepeatingTask repeatingTask;
            if (exceptionOnReRun) {
                repeatingTask = new RepeatingTask(executor, 10, TimeUnit.NANOSECONDS) {
                    @Override
                    protected boolean runAndTest() {
                        try {
                            return runAndTestMethod.call();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }
            else {
                repeatingTask = new RepeatingTask(executor, 10, TimeUnit.NANOSECONDS, false) {
                    @Override
                    protected boolean runAndTest() {
                        try {
                            return runAndTestMethod.call();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }

            if (startImmediately) {
                repeatingTask.execute();
            }
            else {
                repeatingTask.schedule(10, TimeUnit.NANOSECONDS);
            }

            runLatch.await();
        } finally {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verify(task, times(runCount)).run();
    }

    @Test(timeout = 20000)
    public void testSimpleRun() throws InterruptedException {
        for (boolean exception: Arrays.asList(false, true)) {
            for (boolean startImmediately: Arrays.asList(false, true)) {
                for (int runCount: Arrays.asList(1, 2, 5)) {
                    testRun(runCount, exception, startImmediately);
                }
            }
        }
    }

    @Test(timeout = 10000)
    public void testStopScheduleOnException() throws InterruptedException {
        final Runnable task = mock(Runnable.class);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final CountDownLatch wait = new CountDownLatch(1);
        try {
            new RepeatingTask(executor, 0, TimeUnit.NANOSECONDS, false) {
                @Override
                protected boolean runAndTest() {
                    task.run();
                    wait.countDown();
                    throw new RuntimeException();
                }
            }.execute();

            wait.await();
            Thread.sleep(100);
        } finally {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verify(task).run();
    }
}