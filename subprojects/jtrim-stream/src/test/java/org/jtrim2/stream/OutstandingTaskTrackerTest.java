
package org.jtrim2.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.mockito.Mockito.*;

public class OutstandingTaskTrackerTest {
    private static <T> List<T> repeat(int times, T obj) {
        return ProducerConsumerTestUtils.repeat(times, obj);
    }

    private void assumeSignalWorks() {
        // Called for tests who rely on signalling to work to avoid waiting forever
        // If this check is not met, then testFinishWithoutTask() will fail.
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(Tasks.noOpTask());
        tracker.finishAddingTasks();
        assumeTrue(tracker.isFinishedAll());
    }

    @Test(timeout = 10000)
    public void testWaitForAllReturns() {
        assumeSignalWorks();

        OutstandingTaskTracker tracker = new OutstandingTaskTracker(Tasks.noOpTask());
        tracker.finishAddingTasks();
        tracker.waitForAllTasks();
    }

    @Test(timeout = 10000)
    public void testWaitForAllBlocks() throws InterruptedException {
        assumeSignalWorks();

        OutstandingTaskTracker tracker = new OutstandingTaskTracker(Tasks.noOpTask());

        WaitableSignal doneSignal = new WaitableSignal();
        Thread thread = new Thread(() -> {
            tracker.waitForAllTasks();
            doneSignal.signal();
        });

        thread.start();
        try {
            Thread.sleep(10);
            assertFalse("signal-before-done", doneSignal.isSignaled());
        } finally {
            tracker.finishAddingTasks();
            thread.join();
        }

        assertTrue("signal-after-done", doneSignal.isSignaled());
    }

    @Test
    public void testFinishWithoutTask() {
        Runnable onDoneTask = mock(Runnable.class);
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(onDoneTask);

        tracker.finishAddingTasks();

        assertTrue(tracker.isFinishedAll());
        verify(onDoneTask).run();
    }

    private void testFinishWithTask(int numberOfTasks) {
        Runnable onDoneTask = mock(Runnable.class);
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(onDoneTask);

        for (int i = 0; i < numberOfTasks; i++) {
            OutstandingTaskTracker.TaskRef taskRef = tracker.startTask();
            taskRef.finishedTask();
        }

        assertFalse(tracker.isFinishedAll());
        verifyNoMoreInteractions(onDoneTask);

        tracker.finishAddingTasks();

        assertTrue(tracker.isFinishedAll());
        verify(onDoneTask).run();
    }

    @Test
    public void testFinishWithTask() {
        testFinishWithTask(1);
        testFinishWithTask(2);
        testFinishWithTask(5);
    }

    private void testFinishWithTaskReleaseLater(int numberOfTasks) {
        Runnable onDoneTask = mock(Runnable.class);
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(onDoneTask);

        List<OutstandingTaskTracker.TaskRef> taskRefs = new ArrayList<>(numberOfTasks);
        for (int i = 0; i < numberOfTasks; i++) {
            taskRefs.add(tracker.startTask());
        }

        tracker.finishAddingTasks();

        taskRefs.subList(1, numberOfTasks)
                .forEach(OutstandingTaskTracker.TaskRef::finishedTask);

        assertFalse(tracker.isFinishedAll());
        verifyNoMoreInteractions(onDoneTask);

        taskRefs.get(0).finishedTask();

        assertTrue(tracker.isFinishedAll());
        verify(onDoneTask).run();
    }

    @Test
    public void testFinishWithTaskReleaseLater() {
        testFinishWithTaskReleaseLater(1);
        testFinishWithTaskReleaseLater(2);
        testFinishWithTaskReleaseLater(5);
    }

    @Test
    public void testMultipleReleases() {
        Runnable onDoneTask = mock(Runnable.class);
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(onDoneTask);

        OutstandingTaskTracker.TaskRef taskRef1 = tracker.startTask();
        OutstandingTaskTracker.TaskRef taskRef2 = tracker.startTask();
        tracker.finishAddingTasks();

        for (int i = 0; i < 5; i++) {
            taskRef1.finishedTask();
        }

        assertFalse(tracker.isFinishedAll());
        verifyNoMoreInteractions(onDoneTask);

        taskRef2.finishedTask();

        assertTrue(tracker.isFinishedAll());
        verify(onDoneTask).run();
    }

    @Test
    public void testConcurrency() {
        for (int i = 0; i < 100; i++) {
            int threadCount = 2 * Runtime.getRuntime().availableProcessors();

            Runnable onDoneTask = mock(Runnable.class);
            OutstandingTaskTracker tracker = new OutstandingTaskTracker(onDoneTask);

            List<OutstandingTaskTracker.TaskRef> taskRefs = Collections.synchronizedList(new ArrayList<>());
            Tasks.runConcurrently(repeat(threadCount, () -> {
                taskRefs.add(tracker.startTask());
            }));

            assertFalse(tracker.isFinishedAll());
            verifyNoMoreInteractions(onDoneTask);

            List<Runnable> releaseTasks = taskRefs
                    .stream()
                    .<Runnable>map(ref -> ref::finishedTask)
                    .collect(Collectors.toCollection(ArrayList::new));
            releaseTasks.add(tracker::finishAddingTasks);
            Tasks.runConcurrently(releaseTasks);

            assertTrue(tracker.isFinishedAll());
            verify(onDoneTask).run();
        }
    }

    @Test
    public void testIllegalStart() {
        OutstandingTaskTracker tracker = new OutstandingTaskTracker(Tasks.noOpTask());
        tracker.startTask();
        tracker.finishAddingTasks();

        try {
            tracker.startTask();
            fail("Expected failure");
        } catch (IllegalStateException ex) {
            // Expected
        }
    }
}
