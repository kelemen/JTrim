package org.jtrim.access;

import java.util.Collections;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.Tasks;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class RequestGrabberTest {
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

    @Test
    public void testSimple() {
        AccessManager<Object, HierarchicalRight> manager = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        HierarchicalRight right = HierarchicalRight.create(new Object());
        AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest("REQUEST", right);

        RequestGrabber grabber = new RequestGrabber(manager, request);
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.release();
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
        grabber.acquire();
        assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

        grabber.release();
        assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
    }

    @Test(timeout = 20000)
    public void testConcurrent() {
        for (int testIndex = 0; testIndex < 100; testIndex++) {
            AccessManager<Object, HierarchicalRight> manager = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
            HierarchicalRight right = HierarchicalRight.create(new Object());
            AccessRequest<String, HierarchicalRight> request = AccessRequest.getWriteRequest("REQUEST", right);

            final RequestGrabber grabber = new RequestGrabber(manager, request);

            Runnable[] tasks = new Runnable[2 * Runtime.getRuntime().availableProcessors()];
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new Runnable() {
                    @Override
                    public void run() {
                        grabber.acquire();
                    }
                };
            }
            Tasks.runConcurrently(tasks);

            assertFalse(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));

            grabber.release();
            assertTrue(manager.isAvailable(Collections.<HierarchicalRight>emptySet(), Collections.singleton(right)));
        }
    }
}