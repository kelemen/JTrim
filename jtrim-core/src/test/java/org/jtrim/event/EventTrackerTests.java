package org.jtrim.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.hamcrest.Matcher;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.ManualExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.Tasks;
import org.mockito.ArgumentMatcher;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public final class EventTrackerTests {
    private static void invokeTestMethod(Method method, TrackerFactory factory, boolean includeNonGeneric) throws ReflectiveOperationException {
        switch (method.getGenericParameterTypes().length) {
            case 1:
                method.invoke(null, factory);
                break;
            case 2:
                method.invoke(null, factory, includeNonGeneric);
                break;
            default:
                throw new AssertionError("Invalid test method signature: " + method.getName());
        }
    }

    public static void executeAllTests(TrackerFactory factory, boolean includeNonGeneric) throws Throwable {
        Throwable toThrow = null;

        int failureCount = 0;
        for (Method method: EventTrackerTests.class.getMethods()) {
            if (method.isAnnotationPresent(GenericTest.class)) {
                try {
                    invokeTestMethod(method, factory, includeNonGeneric);
                } catch (InvocationTargetException ex) {
                    failureCount++;
                    if (toThrow == null) toThrow = ex.getCause();
                    else toThrow.addSuppressed(ex.getCause());
                }
            }
        }

        if (toThrow != null) {
            throw new RuntimeException(failureCount + " generic test(s) have failed.", toThrow);
        }
    }

    public static void executeTest(String methodName, TrackerFactory factory, boolean includeNonGeneric) throws Throwable {
        try {
            Method method = ListenerManagerTests.class.getMethod(methodName, TrackerFactory.class);
            invokeTestMethod(method, factory, includeNonGeneric);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    @GenericTest
    public static void testEmptyManager(final TrackerFactory factory, boolean includeNonGeneric) throws Exception {
        TrackedListenerManagerTests.executeAllTests(new TrackedListenerManagerTests.ManagerFactory() {
            @Override
            public <ArgType> TrackedListenerManager<ArgType> createEmpty(Class<ArgType> argClass) {
                return factory.createEmpty().getManagerOfType(new Object(), argClass);
            }
        }, includeNonGeneric);
    }

    private static void causeEvents(
            EventTracker tracker,
            final ObjectEventListener listener,
            final Object testArg1,
            final Object testArg2,
            final Executor event2Executor) {

        final TrackedListenerManager<Object> manager = tracker.getManagerOfType(new Object(), Object.class);

        final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(manager.registerListener(new ObjectEventListener() {
            @Override
            public void onEvent(TrackedEvent<Object> trackedEvent) {
                event2Executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listenerRef.unregister();
                        manager.registerListener(listener);
                        manager.onEvent(testArg2);
                    }
                });
            }
        }));
        manager.onEvent(testArg1);
    }

    private static void testGenericExecutorTracks(
            TrackerFactory factory,
            final ExecutorForwarder forwarder) {
        EventTracker tracker = factory.createEmpty();

        ManualExecutor executor = new ManualExecutor();
        final TaskExecutor trackedExecutor = tracker.createTrackedExecutor(executor);

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg1 = new Object();
        final Object testArg2 = new Object();

        causeEvents(tracker, listener, testArg1, testArg2, new Executor() {
            @Override
            public void execute(final Runnable command) {
                forwarder.forwardTask(trackedExecutor, command);
            }
        });
        executor.executeAll();

        verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
        verifyNoMoreInteractions(listener);
    }

    private static void testGenericExecutorServiceTracks(
            TrackerFactory factory,
            final ExecutorServiceForwarder forwarder) {

        EventTracker tracker = factory.createEmpty();

        ManualExecutor executor = new ManualExecutor();
        final TaskExecutorService trackedExecutor = tracker.createTrackedExecutorService(
                TaskExecutors.upgradeExecutor(executor));

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg1 = new Object();
        final Object testArg2 = new Object();

        causeEvents(tracker, listener, testArg1, testArg2, new Executor() {
            @Override
            public void execute(final Runnable command) {
                forwarder.forwardTask(trackedExecutor, command);
            }
        });
        executor.executeAll();

        verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public static void testExecutorTracks(TrackerFactory factory) throws Exception {
        testGenericExecutorTracks(factory, new ExecutorForwarder() {
            @Override
            public void forwardTask(TaskExecutor taskExecutor, final Runnable command) {
                taskExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        command.run();
                    }
                }, null);
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracks1(TrackerFactory factory) {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        command.run();
                    }
                }, null);
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracks2(TrackerFactory factory) throws Exception {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.submit(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        command.run();
                    }
                }, null);
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracks3(TrackerFactory factory) throws Exception {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.submit(Cancellation.UNCANCELABLE_TOKEN, new CancelableFunction<Void>() {
                    @Override
                    public Void execute(CancellationToken cancelToken) {
                        command.run();
                        return null;
                    }
                }, null);
            }
        });
    }

    @GenericTest
    public static void testExecutorTracksInCleanup(TrackerFactory factory) throws Exception {
        testGenericExecutorTracks(factory, new ExecutorForwarder() {
            @Override
            public void forwardTask(TaskExecutor taskExecutor, final Runnable command) {
                taskExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        command.run();
                    }
                });
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracksInCleanup1(TrackerFactory factory) throws Exception {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        command.run();
                    }
                });
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracksInCleanup2(TrackerFactory factory) throws Exception {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.submit(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        command.run();
                    }
                });
            }
        });
    }

    @GenericTest
    public static void testExecutorServiceTracksInCleanup3(TrackerFactory factory) throws Exception {
        testGenericExecutorServiceTracks(factory, new ExecutorServiceForwarder() {
            @Override
            public void forwardTask(TaskExecutorService executorService, final Runnable command) {
                executorService.submit(Cancellation.UNCANCELABLE_TOKEN, new CancelableFunction<Void>() {
                    @Override
                    public Void execute(CancellationToken cancelToken) throws Exception {
                        return null;
                    }
                }, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        command.run();
                    }
                });
            }
        });
    }

    @GenericTest
    public static void testSimpleExecutor(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        final TaskExecutor trackedExecutor = tracker.createTrackedExecutor(SyncTaskExecutor.getSimpleExecutor());

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg = new Object();

        final TrackedListenerManager<Object> manager = tracker.getManagerOfType(new Object(), Object.class);
        manager.registerListener(listener);
        trackedExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                manager.onEvent(testArg);
            }
        }, null);

        verify(listener).onEvent(argThat(eventTrack(testArg)));
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public static void testGetManagerTwice(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        Object eventKind = new Object();

        ObjectEventListener listener = mock(ObjectEventListener.class);
        tracker.getManagerOfType(eventKind, Object.class).registerListener(listener);

        Object testArg = new Object();
        tracker.getManagerOfType(eventKind, Object.class).onEvent(testArg);

        verify(listener).onEvent(argThat(eventTrack(testArg)));
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public static void testIndependentManagers1(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        ObjectEventListener listener = mock(ObjectEventListener.class);

        Object eventKind = new Object();
        tracker.getManagerOfType(eventKind, Object.class).registerListener(listener);
        tracker.getManagerOfType(eventKind, Boolean.class).onEvent(Boolean.TRUE);

        verifyZeroInteractions(listener);
    }

    @GenericTest
    public static void testIndependentManagers2(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        ObjectEventListener listener = mock(ObjectEventListener.class);

        tracker.getManagerOfType(new Object(), Object.class).registerListener(listener);
        tracker.getManagerOfType(new Object(), Object.class).onEvent(new Object());

        verifyZeroInteractions(listener);
    }

    @GenericTest
    public static void testIndependentManagers3(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        ObjectEventListener listener = mock(ObjectEventListener.class);

        tracker.getManagerOfType(new Object(), Object.class).registerListener(listener);
        tracker.getManagerOfType(new Object(), Boolean.class).onEvent(Boolean.TRUE);

        verifyZeroInteractions(listener);
    }

    @GenericTest
    public static void testEventKindOfEvent(TrackerFactory factory) {
        EventTracker tracker = factory.createEmpty();

        final Object eventKind = new Object();

        final TrackedListenerManager<Object> manager = tracker.getManagerOfType(eventKind, Object.class);
        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final Object testArg1 = new Object();
        final Object testArg2 = new Object();

        final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(manager.registerListener(new ObjectEventListener() {
            @Override
            public void onEvent(TrackedEvent<Object> trackedEvent) {
                listenerRef.unregister();
                manager.registerListener(listener);
                manager.onEvent(testArg2);
            }
        }));
        manager.onEvent(testArg1);

        verify(listener).onEvent(argThat(new ArgumentMatcher<TrackedEvent<Object>>() {
            @Override
            public boolean matches(Object argument) {
                @SuppressWarnings("unchecked")
                TrackedEvent<Object> event = (TrackedEvent<Object>)argument;
                if (!Objects.equals(testArg2, event.getEventArg())) {
                    return false;
                }

                EventCauses causes = event.getCauses();
                if (causes.getNumberOfCauses() != 1) {
                    return false;
                }

                TriggeredEvent<?> expected = new TriggeredEvent<>(eventKind, testArg1);
                TriggeredEvent<?> cause = causes.getCauses().iterator().next();

                if (!Objects.equals(expected, cause)) {
                    return false;
                }
                return true;
            }
        }));
    }

    private static Matcher<TrackedEvent<Object>> eventTrack(
            final Object expected, Object... triggeredArgs) {
        return TrackedListenerManagerTests.eventTrack(expected, triggeredArgs);
    }

    private static interface ExecutorForwarder {
        public void forwardTask(TaskExecutor taskExecutor, Runnable command);
    }

    private static interface ExecutorServiceForwarder {
        public void forwardTask(TaskExecutorService executorService, Runnable command);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface GenericTest {
    }

    private interface ObjectEventListener extends TrackedEventListener<Object> {
    }

    public static interface TrackerFactory {
        public EventTracker createEmpty();
    }
}
