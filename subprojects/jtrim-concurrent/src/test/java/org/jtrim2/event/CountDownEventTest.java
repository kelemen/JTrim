package org.jtrim2.event;

import java.util.Arrays;
import org.jtrim2.concurrent.Tasks;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CountDownEventTest {
    private static CountDownEvent create(int counter, Runnable callback) {
        return new CountDownEvent(counter, callback);
    }

    @Test
    public void testSingleDecrement() {
        testDecrement(1);
    }

    @Test
    public void testIllegalDecrement() {
        Runnable handler = mock(Runnable.class);
        CountDownEvent event = create(1, handler);
        event.dec();

        try {
            event.dec();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
        }

        assertEquals(0, event.getCounter());

        verify(handler).run();
    }

    @Test
    public void testIllegalIncrement() {
        Runnable handler = mock(Runnable.class);
        CountDownEvent event = create(1, handler);
        event.dec();

        try {
            event.inc();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
        }

        assertEquals(0, event.getCounter());

        verify(handler).run();
    }

    private void testDecrement(int count) {
        Runnable handler = mock(Runnable.class);
        CountDownEvent event = create(count, handler);

        for (int i = 1; i < count; i++) {
            event.dec();
        }

        verifyNoInteractions(handler);
        event.dec();
        verify(handler).run();

        assertEquals(0, event.getCounter());
    }

    @Test
    public void testMultipleDecrement() {
        testDecrement(2);
        testDecrement(10);
    }

    private void testConcurrentDecrementNow(int threadCount) {
        Runnable handler = mock(Runnable.class);
        CountDownEvent event = create(threadCount, handler);

        Runnable[] tasks = new Runnable[threadCount];
        Arrays.fill(tasks, (Runnable) event::dec);

        Tasks.runConcurrently(tasks);
        verify(handler).run();
    }

    @Test
    public void testConcurrentDecrement() {
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < 100; i++) {
            testConcurrentDecrementNow(threadCount);
        }
    }

    private void testConcurrentDecAndIncNow(int threadCount) {
        int incThreadCount = Math.max(1, threadCount / 2);
        int decThreadCount = Math.max(1, threadCount - incThreadCount);

        Runnable handler = mock(Runnable.class);
        CountDownEvent event = create(decThreadCount + 1, handler);

        Runnable[] tasks = new Runnable[decThreadCount + incThreadCount];
        Arrays.fill(tasks, 0, decThreadCount, (Runnable) event::dec);
        Arrays.fill(tasks, decThreadCount, tasks.length, (Runnable) event::inc);

        Tasks.runConcurrently(tasks);
        verifyNoInteractions(handler);

        int expectedCounter = incThreadCount + 1;
        for (int i = 0; i < expectedCounter; i++) {
            event.dec();
        }
        verify(handler).run();
    }

    @Test
    public void testConcurrentDecAndInc() {
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < 100; i++) {
            testConcurrentDecAndIncNow(threadCount);
        }
    }
}
