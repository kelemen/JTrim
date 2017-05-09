package org.jtrim2.property;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ConcurrentMemPropertyTest {
    @SuppressWarnings("unchecked")
    private static PropertyVerifier<Object> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    @SuppressWarnings("unchecked")
    private static PropertyPublisher<Object> mockPublisher() {
        return mock(PropertyPublisher.class);
    }

    private static ConcurrentMemProperty<Object> createSimple(Object initialValue) {
        return createSimple(initialValue, SyncTaskExecutor.getSimpleExecutor());
    }

    private static ConcurrentMemProperty<Object> createSimple(Object initialValue, TaskExecutor eventExecutor) {
        return new ConcurrentMemProperty<>(
                initialValue,
                NoOpVerifier.getInstance(),
                NoOpPublisher.getInstance(),
                eventExecutor);
    }

    @Test(expected = NullPointerException.class)
    public void testNullExecutor() {
        createSimple(new Object(), null);
    }

    @Test
    public void testInitialValue() {
        Object initialValue = new Object();
        ConcurrentMemProperty<Object> property = createSimple(initialValue);
        assertSame(initialValue, property.getValue());
    }

    @Test
    public void testSetValue() {
        ConcurrentMemProperty<Object> property = createSimple(new Object());

        Object newValue = new Object();
        property.setValue(newValue);
        assertSame(newValue, property.getValue());
    }

    @Test
    public void testListener() {
        ConcurrentMemProperty<Object> property = createSimple(new Object());
        MemPropertyTest.testListener(property);
    }

    @Test
    public void testVerifier() {
        Object verified1 = new Object();
        Object verified2 = new Object();

        PropertyVerifier<Object> verifier = mockVerifier();
        stub(verifier.storeValue(any()))
                .toReturn(verified1)
                .toReturn(verified2)
                .toReturn(new Object());

        Object value1 = new Object();
        Object value2 = new Object();

        ConcurrentMemProperty<Object> property = new ConcurrentMemProperty<>(
                value1, verifier, NoOpPublisher.getInstance(), SyncTaskExecutor.getSimpleExecutor());
        verify(verifier).storeValue(any());
        assertSame(verified1, property.getValue());

        property.setValue(value2);
        assertSame(verified2, property.getValue());

        ArgumentCaptor<Object> verifierArgs = ArgumentCaptor.forClass(Object.class);
        verify(verifier, times(2)).storeValue(verifierArgs.capture());

        assertEquals(Arrays.asList(value1, value2), verifierArgs.getAllValues());
    }

    @Test
    public void testPublisher() {
        Object published1 = new Object();
        Object published2 = new Object();

        PropertyPublisher<Object> publisher = mockPublisher();
        stub(publisher.returnValue(any()))
                .toReturn(published1)
                .toReturn(published2)
                .toReturn(new Object());

        Object value1 = new Object();
        Object value2 = new Object();

        ConcurrentMemProperty<Object> property = new ConcurrentMemProperty<>(
                value1, NoOpVerifier.getInstance(), publisher, SyncTaskExecutor.getSimpleExecutor());
        verifyZeroInteractions(publisher);

        assertSame(published1, property.getValue());
        verify(publisher).returnValue(any());

        property.setValue(value2);
        assertSame(published2, property.getValue());

        ArgumentCaptor<Object> publisherArgs = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).returnValue(publisherArgs.capture());

        assertEquals(Arrays.asList(value1, value2), publisherArgs.getAllValues());
    }

    @Test
    public void testListenerExecutorInContext() {
        final ContextAwareTaskExecutor executor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
        ConcurrentMemProperty<Object> property = createSimple(
                new Object(), executor);

        Runnable listener = mock(Runnable.class);

        final AtomicBoolean inContext = new AtomicBoolean(false);
        doAnswer((InvocationOnMock invocation) -> {
            inContext.set(executor.isExecutingInThis());
            return null;
        }).when(listener).run();

        property.addChangeListener(listener);

        property.setValue(new Object());
        verify(listener).run();

        assertTrue(inContext.get());
    }

    @Test
    public void testMergeEvents() {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);
        ConcurrentMemProperty<Object> property = createSimple(
                new Object(), executor);

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);

        property.setValue(new Object());
        property.setValue(new Object());

        executor.executeCurrentlySubmitted();
        verify(listener).run();
    }

    private void runSingleConcurrentWritesTest() {
        Object originalValue = new Object();
        final ConcurrentMemProperty<Object> property = createSimple(
                originalValue, SyncTaskExecutor.getSimpleExecutor());

        Runnable[] writeTasks = new Runnable[2 * Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < writeTasks.length; i++) {
            writeTasks[i] = () -> property.setValue(new Object());
        }

        final AtomicReference<Object> lastReadValueRef = new AtomicReference<>();
        property.addChangeListener(() -> {
            lastReadValueRef.set(property.getValue());
        });

        Tasks.runConcurrently(writeTasks);

        Object lastReadValue = lastReadValueRef.get();
        assertSame(property.getValue(), lastReadValue);
        assertNotSame(originalValue, lastReadValue);
    }

    @Test
    public void testConcurrentWrites() {
        for (int i = 0; i < 100; i++) {
            runSingleConcurrentWritesTest();
        }
    }
}
