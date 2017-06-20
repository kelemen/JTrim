package org.jtrim2.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jtrim2.concurrent.Tasks;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ProxyListenerRegistryTest {
    private static <T> ListenerManager<T> createBackingRegistry() {
        return new CopyOnTriggerListenerManager<>();
    }

    @Test
    public void testInitialBackingRegistry() {
        ListenerManager<Runnable> backingRegistry = createBackingRegistry();
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(backingRegistry);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);

        verifyZeroInteractions(listener);
        EventListeners.dispatchRunnable(backingRegistry);
        verify(listener).run();

        listenerRef.unregister();
        EventListeners.dispatchRunnable(backingRegistry);
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

        EventListeners.dispatchRunnable(initialRegistry);
        verifyZeroInteractions(listener);

        EventListeners.dispatchRunnable(replacingRegistry);
        verify(listener).run();

        listenerRef.unregister();
        EventListeners.dispatchRunnable(replacingRegistry);
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
        EventListeners.dispatchRunnable(backingRegistry);
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

        EventListeners.dispatchRunnable(initialRegistry);
        verifyZeroInteractions(listener1, listener2);

        EventListeners.dispatchRunnable(replacingRegistry);
        verify(listener1).run();
        verify(listener2, never()).run();
    }

    @Test
    public void testReplacingBackingRegistryWithListenerAlreadyAdded() {
        ListenerManager<Runnable> initialRegistry = createBackingRegistry();
        ListenerManager<Runnable> replacingRegistry = createBackingRegistry();

        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(initialRegistry);
        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);

        proxy.replaceRegistry(replacingRegistry);

        EventListeners.dispatchRunnable(initialRegistry);
        verifyZeroInteractions(listener);

        EventListeners.dispatchRunnable(replacingRegistry);
        verify(listener).run();

        listenerRef.unregister();
        EventListeners.dispatchRunnable(replacingRegistry);
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
            replaceTasks[i] = () -> {
                proxy.replaceRegistry(manager);
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

        EventListeners.dispatchRunnable(initialRegistry);
        for (ListenerManager<Runnable> manager: managers) {
            EventListeners.dispatchRunnable(manager);
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

    private static void testSimpleOnEvent(int numberOfTasks) {
        ListenerManager<TaskWithArg> backingRegistry = createBackingRegistry();
        ProxyListenerRegistry<TaskWithArg> proxy = new ProxyListenerRegistry<>(backingRegistry);
        TaskWithArg[] tasks = new TaskWithArg[numberOfTasks];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = mock(TaskWithArg.class);
            proxy.registerListener(tasks[i]);
        }

        Object arg = new Object();
        proxy.onEvent(TaskWithArg::run, arg);

        for (TaskWithArg task: tasks) {
            verify(task).run(same(arg));
        }
    }

    @Test
    public void testSimpleOnEvent() {
        for (int numberOfTasks = 0; numberOfTasks < 5; numberOfTasks++) {
            testSimpleOnEvent(numberOfTasks);
        }
    }

    @Test
    public void testErrorEvents() {
        TaskWithArg task1 = mock(TaskWithArg.class);
        TaskWithArg task2 = mock(TaskWithArg.class);
        TaskWithArg task3 = mock(TaskWithArg.class);

        TestException ex1 = new TestException();
        TestException ex2 = new TestException();
        TestException ex3 = new TestException();

        doThrow(ex1).when(task1).run(any());
        doThrow(ex2).when(task2).run(any());
        doThrow(ex3).when(task3).run(any());

        ListenerManager<TaskWithArg> backingRegistry = createBackingRegistry();
        ProxyListenerRegistry<TaskWithArg> proxy = new ProxyListenerRegistry<>(backingRegistry);

        proxy.registerListener(task1);
        proxy.registerListener(task2);
        proxy.registerListener(task3);

        Object arg = new Object();
        try {
            proxy.onEvent(TaskWithArg::run, arg);
            fail("Expected: TestException");
        } catch (TestException ex) {
            assertTrue(ex == ex1 || ex == ex2 || ex == ex3);

            Throwable[] suppressed = ex.getSuppressed();
            assertEquals(2, suppressed.length);

            Set<Throwable> received = new HashSet<>();
            received.add(ex);
            received.addAll(Arrays.asList(suppressed));

            Set<Throwable> expected = new HashSet<>(Arrays.asList(ex1, ex2, ex3));
            assertEquals(expected, received);
        }

        verify(task1).run(same(arg));
        verify(task2).run(same(arg));
        verify(task3).run(same(arg));
    }

    @Test
    public void testReplaceAfterUnregisterStillNotifiesListeners() {
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(dummyListenerRegistry());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.registerListener(listener);

        ListenerManager<Runnable> wrapped = createBackingRegistry();
        proxy.replaceRegistry(wrapped);

        verifyZeroInteractions(listener);

        EventListeners.dispatchRunnable(wrapped);
        verify(listener).run();

        proxy.onEvent(EventListeners.runnableDispatcher(), null);
        verify(listener, times(2)).run();

        listenerRef.unregister();

        assertEquals(0, proxy.getNumberOfProxiedListeners());

        EventListeners.dispatchRunnable(wrapped);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testListenerIsNotForgottenAfterUsingADummyRegistry() {
        ProxyListenerRegistry<Runnable> proxy = new ProxyListenerRegistry<>(createBackingRegistry());

        Runnable listener = mock(Runnable.class);
        proxy.registerListener(listener);

        proxy.replaceRegistry(dummyListenerRegistry());

        verifyZeroInteractions(listener);

        proxy.onEvent(EventListeners.runnableDispatcher(), null);

        verify(listener).run();
    }

    private static SimpleListenerRegistry<Runnable> dummyListenerRegistry() {
        return (listener) -> ListenerRefs.unregistered();
    }

    private static interface TaskWithArg {
        public void run(Object arg);
    }

    @SuppressWarnings("serial")
    private static class TestException extends RuntimeException {
    }
}
