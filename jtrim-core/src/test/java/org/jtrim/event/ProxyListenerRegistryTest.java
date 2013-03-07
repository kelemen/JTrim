package org.jtrim.event;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.concurrent.Tasks;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ProxyListenerRegistryTest {
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

    private static ListenerManager<Runnable> createBackingRegistry() {
        return new CopyOnTriggerListenerManager<>();
    }

    @Test
    public void testInitialBackingRegistry() {
        ListenerManager<Runnable> backingRegistry = createBackingRegistry();
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(backingRegistry);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);
        assertTrue(listenerRef.isRegistered());

        verifyZeroInteractions(listener);
        backingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        backingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();
    }

    @Test
    public void testReplacingBackingRegistry() {
        ListenerManager<Runnable> initialRegistry = createBackingRegistry();
        ListenerManager<Runnable> replacingRegistry = createBackingRegistry();

        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(initialRegistry);
        proxy.replaceRegistry(replacingRegistry);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);
        assertTrue(listenerRef.isRegistered());

        initialRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verifyZeroInteractions(listener);

        replacingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        replacingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();
    }

    @Test
    public void testInitialBackingRegistryRemovingListenerNotAffectsOthers() {
        ListenerManager<Runnable> backingRegistry = createBackingRegistry();
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(backingRegistry);

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);
        proxy.registerListener(listener1);
        proxy.registerListener(listener2).unregister();

        verifyZeroInteractions(listener1, listener2);
        backingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener1).run();
        verify(listener2, never()).run();
    }

    @Test
    public void testReplacingBackingRegistryRemovingListenerNotAffectsOthers() {
        ListenerManager<Runnable> initialRegistry = createBackingRegistry();
        ListenerManager<Runnable> replacingRegistry = createBackingRegistry();

        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(initialRegistry);
        proxy.replaceRegistry(replacingRegistry);

        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);
        proxy.registerListener(listener1);
        proxy.registerListener(listener2).unregister();

        initialRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verifyZeroInteractions(listener1, listener2);

        replacingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener1).run();
        verify(listener2, never()).run();
    }

    @Test
    public void testImmediatelyUnregister() {
        SimpleListenerRegistry<Runnable> backingRegistry = DummyListenerRegistry.INSTANCE;
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(backingRegistry);

        ListenerRef listenerRef = proxy.registerListener(mock(Runnable.class));
        assertFalse(listenerRef.isRegistered());

        assertEquals(0, proxy.getNumberOfProxiedListeners());
    }

    @Test
    public void testReplacingBackingRegistryWithListenerAlreadyAdded() {
        ListenerManager<Runnable> initialRegistry = createBackingRegistry();
        ListenerManager<Runnable> replacingRegistry = createBackingRegistry();

        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(initialRegistry);
        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);

        proxy.replaceRegistry(replacingRegistry);

        assertTrue(listenerRef.isRegistered());

        initialRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verifyZeroInteractions(listener);

        replacingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
        replacingRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        verify(listener).run();
    }

    private void testConcurrentReplace(int numberOfThreads) {
        assert numberOfThreads > 0;

        ListenerManager<Runnable> initialRegistry = createBackingRegistry();
        final ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(initialRegistry);

        Runnable[] initialTasks = new Runnable[]{
            mock(Runnable.class),
            mock(Runnable.class),
            mock(Runnable.class)};
        for (Runnable task: initialTasks) {
            proxy.registerListener(task);
        }

        List<ListenerManager<Runnable>> managers = new ArrayList<>();
        Runnable[] replaceTasks = new Runnable[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            final ListenerManager<Runnable> manager = createBackingRegistry();
            managers.add(manager);
            replaceTasks[i] = new Runnable() {
                @Override
                public void run() {
                    proxy.replaceRegistry(manager);
                }
            };
        }

        Tasks.runConcurrently(replaceTasks);

        Runnable[] lateTasks = new Runnable[]{
            mock(Runnable.class),
            mock(Runnable.class),
            mock(Runnable.class)};
        for (Runnable task: lateTasks) {
            proxy.registerListener(task);
        }

        initialRegistry.onEvent(RunnableDispatcher.INSTANCE, null);
        for (ListenerManager<Runnable> manager: managers) {
            manager.onEvent(RunnableDispatcher.INSTANCE, null);
        }

        for (Runnable task: initialTasks) {
            verify(task).run();
        }
        for (Runnable task: lateTasks) {
            verify(task).run();
        }
    }

    @Test
    public void testConcurrentReplace() {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrentReplace(numberOfThreads);
        }
    }

    private enum DummyListenerRegistry implements SimpleListenerRegistry<Runnable> {
        INSTANCE;

        @Override
        public ListenerRef registerListener(Runnable listener) {
            return UnregisteredListenerRef.INSTANCE;
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
