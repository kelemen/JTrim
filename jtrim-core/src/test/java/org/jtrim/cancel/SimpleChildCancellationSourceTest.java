package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.event.ListenerRef;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class SimpleChildCancellationSourceTest {

    public SimpleChildCancellationSourceTest() {
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
    public void testInitialCanceled() {
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testInitialNotCanceled() {
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        assertFalse(child.getToken().isCanceled());
    }

    @Test
    public void testCancel() {
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        child.getController().cancel();
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testCancelAfterCreateByParent() {
        CancellationSource source = Cancellation.createCancellationSource();
        CancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        assertTrue(child instanceof SimpleChildCancellationSource);
        source.getController().cancel();
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testCancelBeforeCreateByParent() {
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testListenerBeforeCreateByParent() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerAfterCreateByParent() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource source = Cancellation.createCancellationSource();
        CancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        assertTrue(child instanceof SimpleChildCancellationSource);
        source.getController().cancel();
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerAfterAddByParent() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource source = Cancellation.createCancellationSource();
        CancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        assertTrue(child instanceof SimpleChildCancellationSource);
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        source.getController().cancel();
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerAfterAdd() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        child.getController().cancel();
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerMultipleCancel() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        child.getController().cancel();
        child.getController().cancel();
        assertEquals(1, invoked.get());
    }

    @Test
    public void testListenerUnregister() {
        final AtomicInteger invoked = new AtomicInteger(0);
        CancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(child instanceof SimpleChildCancellationSource);
        ListenerRef ref = child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertTrue(ref.isRegistered());
        ref.unregister();
        assertFalse(ref.isRegistered());

        child.getController().cancel();

        assertEquals(0, invoked.get());
    }
}
