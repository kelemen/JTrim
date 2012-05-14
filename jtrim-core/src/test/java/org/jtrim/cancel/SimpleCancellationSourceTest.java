package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.event.ListenerRef;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class SimpleCancellationSourceTest {

    public SimpleCancellationSourceTest() {
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

    @Test
    public void testInitialNotCanceled() {
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        assertFalse(cancelSource.getToken().isCanceled());
    }

    @Test
    public void testCancel() {
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cancelSource.getController().cancel();
        assertTrue(cancelSource.getToken().isCanceled());
    }

    @Test
    public void testListenerBeforeAdd() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cancelSource.getController().cancel();
        cancelSource.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get());
    }


    @Test
    public void testListenerAfterAdd() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cancelSource.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        cancelSource.getController().cancel();
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerMultipleCancel() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        cancelSource.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        cancelSource.getController().cancel();
        cancelSource.getController().cancel();
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerUnregister() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource cancelSource = Cancellation.createCancellationSource();
        ListenerRef ref = cancelSource.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertTrue(ref.isRegistered());
        ref.unregister();
        assertFalse(ref.isRegistered());

        cancelSource.getController().cancel();

        assertEquals(0, invoked.get());
    }
}
