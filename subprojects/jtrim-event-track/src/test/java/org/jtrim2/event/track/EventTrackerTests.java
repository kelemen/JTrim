package org.jtrim2.event.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.InitLaterListenerRef;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.testutils.FactoryTestMethod;
import org.junit.Test;

import static org.mockito.Mockito.*;

public abstract class EventTrackerTests extends TrackedListenerManagerTests {
    private final List<TrackerFactory> factories;

    public EventTrackerTests(Collection<? extends TrackerFactory> factories) {
        super(fromTrackerFactoryAll(factories));

        this.factories = CollectionsEx.readOnlyCopy(factories);
    }

    protected final void testAllTrackers(FactoryTestMethod<TrackerFactory> testMethod) throws Exception {
        for (TrackerFactory factory: factories) {
            testMethod.doTest(factory);
        }
    }

    private static List<TrackedManagerFactory> fromTrackerFactoryAll(Collection<? extends TrackerFactory> factories) {
        List<TrackedManagerFactory> result = new ArrayList<>(factories.size());
        factories.forEach((factory) -> {
            result.add(fromTrackerFactory(factory));
        });
        return result;
    }

    private static TrackedManagerFactory fromTrackerFactory(TrackerFactory factory) {
        return new TrackedManagerFactory() {
            @Override
            public <ArgType> TrackedListenerManager<ArgType> createEmpty(Class<ArgType> argClass) {
                return factory.createEmpty().getManagerOfType(new Object(), argClass);
            }
        };
    }

    private static void causeEvents(
            EventTracker tracker,
            final ObjectEventListener listener,
            final Object testArg1,
            final Object testArg2,
            final Executor event2Executor) {

        final TrackedListenerManager<Object> manager = tracker.getManagerOfType(new Object(), Object.class);

        final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(manager.registerListener((TrackedEvent<Object> trackedEvent) -> {
            event2Executor.execute(() -> {
                listenerRef.unregister();
                manager.registerListener(listener);
                manager.onEvent(testArg2);
            });
        }));
        manager.onEvent(testArg1);
    }

    private static void testGenericExecutorTracks(
            TrackerFactory factory,
            final ExecutorForwarder forwarder) {
        EventTracker tracker = factory.createEmpty();

        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        final TaskExecutor trackedExecutor = tracker.createTrackedExecutor(executor);

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg1 = new Object();
        final Object testArg2 = new Object();

        causeEvents(tracker, listener, testArg1, testArg2, (final Runnable command) -> {
            forwarder.forwardTask(trackedExecutor, command);
        });
        executor.executeCurrentlySubmitted();

        verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
        verifyNoMoreInteractions(listener);
    }

    private static void testGenericExecutorServiceTracks(
            TrackerFactory factory,
            final ExecutorServiceForwarder forwarder) {

        EventTracker tracker = factory.createEmpty();

        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        final TaskExecutorService trackedExecutor = tracker.createTrackedExecutorService(
                TaskExecutors.upgradeToUnstoppable(executor));

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg1 = new Object();
        final Object testArg2 = new Object();

        causeEvents(tracker, listener, testArg1, testArg2, (final Runnable command) -> {
            forwarder.forwardTask(trackedExecutor, command);
        });
        executor.executeCurrentlySubmitted();

        verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testExecutorTracks1() throws Exception {
        testAllTrackers((factory) -> {
            testGenericExecutorTracks(factory, (TaskExecutor taskExecutor, final Runnable command) -> {
                taskExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    command.run();
                });
            });
        });
    }

    @Test
    public void testExecutorTracks2() throws Exception {
        testAllTrackers((factory) -> {
            testGenericExecutorTracks(factory, (TaskExecutor taskExecutor, final Runnable command) -> {
                taskExecutor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    command.run();
                    return null;
                });
            });
        });
    }

    @Test
    public void testExecutorTracks3() throws Exception {
        testAllTrackers((factory) -> {
            testGenericExecutorTracks(factory, (TaskExecutor taskExecutor, final Runnable command) -> {
                taskExecutor.execute(command);
            });
        });
    }

    @Test
    public void testSimpleExecutor() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            final TaskExecutor trackedExecutor = tracker.createTrackedExecutor(SyncTaskExecutor.getSimpleExecutor());

            final ObjectEventListener listener = mock(ObjectEventListener.class);

            final Object testArg = new Object();

            final TrackedListenerManager<Object> manager = tracker.getManagerOfType(new Object(), Object.class);
            manager.registerListener(listener);
            trackedExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                manager.onEvent(testArg);
            });

            verify(listener).onEvent(argThat(eventTrack(testArg)));
            verifyNoMoreInteractions(listener);
        });
    }

    @Test
    public void testGetManagerTwice() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            Object eventKind = new Object();

            ObjectEventListener listener = mock(ObjectEventListener.class);
            tracker.getManagerOfType(eventKind, Object.class).registerListener(listener);

            Object testArg = new Object();
            tracker.getManagerOfType(eventKind, Object.class).onEvent(testArg);

            verify(listener).onEvent(argThat(eventTrack(testArg)));
            verifyNoMoreInteractions(listener);
        });
    }

    @Test
    public void testIndependentManagers1() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            ObjectEventListener listener = mock(ObjectEventListener.class);

            Object eventKind = new Object();
            tracker.getManagerOfType(eventKind, Object.class).registerListener(listener);
            tracker.getManagerOfType(eventKind, Boolean.class).onEvent(Boolean.TRUE);

            verifyNoInteractions(listener);
        });
    }

    @Test
    public void testIndependentManagers2() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            ObjectEventListener listener = mock(ObjectEventListener.class);

            tracker.getManagerOfType(new Object(), Object.class).registerListener(listener);
            tracker.getManagerOfType(new Object(), Object.class).onEvent(new Object());

            verifyNoInteractions(listener);
        });
    }

    @Test
    public void testIndependentManagers3() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            ObjectEventListener listener = mock(ObjectEventListener.class);

            tracker.getManagerOfType(new Object(), Object.class).registerListener(listener);
            tracker.getManagerOfType(new Object(), Boolean.class).onEvent(Boolean.TRUE);

            verifyNoInteractions(listener);
        });
    }

    @Test
    public void testEventKindOfEvent() throws Exception {
        testAllTrackers((factory) -> {
            EventTracker tracker = factory.createEmpty();

            final Object eventKind = new Object();

            final TrackedListenerManager<Object> manager = tracker.getManagerOfType(eventKind, Object.class);
            final ObjectEventListener listener = mock(ObjectEventListener.class);

            final Object testArg1 = new Object();
            final Object testArg2 = new Object();

            final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
            listenerRef.init(manager.registerListener((TrackedEvent<Object> trackedEvent) -> {
                listenerRef.unregister();
                manager.registerListener(listener);
                manager.onEvent(testArg2);
            }));
            manager.onEvent(testArg1);

            verify(listener).onEvent(argThat(argument -> {
                if (!Objects.equals(testArg2, argument.getEventArg())) {
                    return false;
                }

                EventCauses causes = argument.getCauses();
                if (causes.getNumberOfCauses() != 1) {
                    return false;
                }

                TriggeredEvent<?> expected = new TriggeredEvent<>(eventKind, testArg1);
                TriggeredEvent<?> cause = causes.getCauses().iterator().next();

                return Objects.equals(expected, cause);
            }));
        });
    }

    private static interface ExecutorForwarder {
        public void forwardTask(TaskExecutor taskExecutor, Runnable command);
    }

    private static interface ExecutorServiceForwarder {
        public void forwardTask(TaskExecutorService executorService, Runnable command);
    }

    private interface ObjectEventListener extends TrackedEventListener<Object> {
    }

    public static interface TrackerFactory {
        public EventTracker createEmpty();
    }
}
