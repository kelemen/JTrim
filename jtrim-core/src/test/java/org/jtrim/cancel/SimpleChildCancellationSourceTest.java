package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
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
    public void testParent() {
        CancellationSource source = Cancellation.createCancellationSource();
        ChildCancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        assertSame(source.getToken(), child.getParentToken());
    }

    @Test
    public void testInitialCanceled() {
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testInitialNotCanceled() {
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        assertFalse(child.getToken().isCanceled());
    }

    @Test
    public void testCancel() {
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        child.getController().cancel();
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testCancelAfterCreateByParent() {
        CancellationSource source = Cancellation.createCancellationSource();
        ChildCancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        source.getController().cancel();
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testCancelBeforeCreateByParent() {
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
        assertTrue(child.getToken().isCanceled());
    }

    @Test
    public void testDetachParent() {
        CancellationSource source = Cancellation.createCancellationSource();
        ChildCancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
        child.detachFromParent();
        source.getController().cancel();
        assertFalse(child.getToken().isCanceled());
    }

    @Test
    public void testListenerBeforeCreateByParent() {
        final AtomicInteger invoked = new AtomicInteger(0);
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.CANCELED_TOKEN);
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
        ChildCancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
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
        ChildCancellationSource child = Cancellation.createChildCancellationSource(source.getToken());
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
        ChildCancellationSource child = Cancellation.createChildCancellationSource(Cancellation.UNCANCELABLE_TOKEN);
        child.getToken().addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        child.getController().cancel();
        assertEquals(1, invoked.get());
    }
}
