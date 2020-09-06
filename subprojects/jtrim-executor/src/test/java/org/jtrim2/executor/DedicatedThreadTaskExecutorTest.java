package org.jtrim2.executor;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.executor.GenericExecutorTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;

import static org.junit.Assert.*;

public class DedicatedThreadTaskExecutorTest {
    private static void verifyThreadName(String expectedName) {
        String name = Thread.currentThread().getName();
        if (!name.contains(expectedName)) {
            throw new AssertionError("Thread name execpted to contain " + expectedName + " but was " + name);
        }
    }

    public abstract static class AbstractNewThreadTaskExecutorTest extends GenericExecutorTests<TaskExecutor> {
        public AbstractNewThreadTaskExecutorTest(Supplier<? extends TaskExecutor> factory) {
            super(false, Collections.<TestExecutorFactory<TaskExecutor>>singleton(
                    new TestExecutorFactory<>(factory, executor -> { })
            ));
        }
    }

    public abstract static class AbstractVerifiedNewThreadTaskExecutorTest
    extends
            AbstractNewThreadTaskExecutorTest {

        private final Runnable verification;

        public AbstractVerifiedNewThreadTaskExecutorTest(
                Supplier<? extends TaskExecutor> factory,
                Runnable verification) {

            super(factory);

            this.verification = Objects.requireNonNull(verification, "verification");
        }

        @Test
        public void testThreadContext() throws Exception {
            testAll(factory -> {
                Throwable error = factory.runTest(executor -> {
                    WaitableSignal doneSignal = new WaitableSignal();
                    AtomicReference<Throwable> verificationError = new AtomicReference<>(null);
                    executor.execute(() -> {
                        try {
                            verification.run();
                        } catch (Throwable ex) {
                            verificationError.set(ex);
                        } finally {
                            doneSignal.signal();
                        }
                    });
                    return verificationError.get();
                });

                ExceptionHelper.rethrowCheckedIfNotNull(error, Exception.class);
            });
        }
    }

    public static class DaemonExecutorTests1 extends AbstractVerifiedNewThreadTaskExecutorTest {
        public DaemonExecutorTests1() {
            super(
                    () -> TaskExecutors.newThreadExecutor(true),
                    () -> {
                        assertTrue("daemon", Thread.currentThread().isDaemon());
                    }
            );
        }
    }

    public static class DaemonExecutorTests2 extends AbstractVerifiedNewThreadTaskExecutorTest {
        public DaemonExecutorTests2() {
            super(
                    () -> TaskExecutors.newThreadExecutor(true, "MyDaemonTestExecutor"),
                    () -> {
                        assertTrue("daemon", Thread.currentThread().isDaemon());
                        verifyThreadName("MyDaemonTestExecutor");
                    }
            );
        }
    }

    public static class NonDaemonExecutorTests1 extends AbstractVerifiedNewThreadTaskExecutorTest {
        public NonDaemonExecutorTests1() {
            super(
                    () -> TaskExecutors.newThreadExecutor(false),
                    () -> {
                        assertFalse("daemon", Thread.currentThread().isDaemon());
                    }
            );
        }
    }

    public static class NonDaemonExecutorTests2 extends AbstractVerifiedNewThreadTaskExecutorTest {
        public NonDaemonExecutorTests2() {
            super(
                    () -> TaskExecutors.newThreadExecutor(true, "MyNonDaemonTestExecutor"),
                    () -> {
                        assertFalse("daemon", Thread.currentThread().isDaemon());
                        verifyThreadName("MyNonDaemonTestExecutor");
                    }
            );
        }
    }
}
