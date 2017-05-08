package org.jtrim2.access;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CleanupTask;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CombinedTokenTest {
    private static final AtomicLong CURRENT_INDEX = new AtomicLong(0);

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

    private static <IDType> CombinedToken<IDType> create(IDType id, AccessToken<?>... tokens) {
        return new CombinedToken<>(id, tokens);
    }

    private static CombinedToken<String> create(AccessToken<?>... tokens) {
        return new CombinedToken<>("COMBINED-TOKEN", tokens);
    }

    private static AccessToken<?>[] createTokens(int numberOfTokens) {
        AccessToken<?>[] result = new AccessToken<?>[numberOfTokens];
        for (int i = 0; i < result.length; i++) {
            result[i] = AccessTokens.createToken("TOKEN-" + CURRENT_INDEX.getAndIncrement());
        }
        return result;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor1() {
        create();
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(null, createTokens(2));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor3() {
        AccessToken<?>[] tokens = createTokens(3);
        tokens[0] = null;
        create(tokens);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor4() {
        AccessToken<?>[] tokens = createTokens(3);
        tokens[1] = null;
        create(tokens);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor5() {
        AccessToken<?>[] tokens = createTokens(3);
        tokens[2] = null;
        create(tokens);
    }

    /**
     * Test of getAccessID method, of class CombinedToken.
     */
    @Test
    public void testGetAccessID() {
        Object accessID = new Object();
        CombinedToken<Object> token = create(accessID, createTokens(2));
        assertSame(accessID, token.getAccessID());
    }

    @Test
    public void testContext() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
            final CombinedToken<String> combined = create(subTokens);

            TaskExecutor executor = combined.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            final boolean[] inContext = new boolean[numberOfTokens];
            final AtomicBoolean selfContext = new AtomicBoolean(false);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                selfContext.set(combined.isExecutingInThis());
                for (int i = 0; i < subTokens.length; i++) {
                    inContext[i] = subTokens[i].isExecutingInThis();
                }
            }, null);

            assertTrue("SELF CONTEXT", selfContext.get());
            assertFalse(combined.isExecutingInThis());
            for (int i = 0; i < inContext.length; i++) {
                assertTrue("TOKEN CONTEXT - " + i, inContext[i]);
            }
        }
    }

    @Test
    public void testContextWithCleanup() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
            final CombinedToken<String> combined = create(subTokens);

            TaskExecutor executor = combined.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            final boolean[] inContext1 = new boolean[numberOfTokens];
            final boolean[] inContext2 = new boolean[numberOfTokens];
            final AtomicBoolean selfContext1 = new AtomicBoolean(false);
            final AtomicBoolean selfContext2 = new AtomicBoolean(false);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                selfContext1.set(combined.isExecutingInThis());
                for (int i = 0; i < subTokens.length; i++) {
                    inContext1[i] = subTokens[i].isExecutingInThis();
                }
            }, (boolean canceled, Throwable error) -> {
                selfContext2.set(combined.isExecutingInThis());
                for (int i = 0; i < subTokens.length; i++) {
                    inContext2[i] = subTokens[i].isExecutingInThis();
                }
            });

            assertTrue("SELF CONTEXT", selfContext1.get());
            assertTrue("SELF CONTEXT", selfContext2.get());
            assertFalse(combined.isExecutingInThis());
            for (int i = 0; i < inContext1.length; i++) {
                assertTrue("TOKEN CONTEXT - " + i, inContext1[i]);
                assertTrue("TOKEN CONTEXT - " + i, inContext2[i]);
            }
        }
    }

    @Test
    public void testSubTokensNotContext() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
            final CombinedToken<String> combined = create(subTokens);

            for (int i = 0; i < subTokens.length; i++) {
                final AtomicBoolean selfContext1 = new AtomicBoolean(true);
                final AtomicBoolean selfContext2 = new AtomicBoolean(true);

                TaskExecutor executor = subTokens[i].createExecutor(SyncTaskExecutor.getSimpleExecutor());
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    selfContext1.set(combined.isExecutingInThis());
                }, (boolean canceled, Throwable error) -> {
                    selfContext2.set(combined.isExecutingInThis());
                });

                assertFalse(selfContext1.get());
                assertFalse(selfContext2.get());
            }
        }
    }

    @Test
    public void testReleasedPriorCreateCombined() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int releaseIndex = 0; releaseIndex < numberOfTokens; releaseIndex++) {
                final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
                subTokens[releaseIndex].release();

                final CombinedToken<String> combined = create(subTokens);
                assertTrue(combined.isReleased());

                Runnable listener = mock(Runnable.class);
                combined.addReleaseListener(listener);
                verify(listener).run();
            }
        }
    }

    @Test
    public void testReleaseOfSubtokenReleasesCombined() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int releaseIndex = 0; releaseIndex < numberOfTokens; releaseIndex++) {
                final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
                final CombinedToken<String> combined = create(subTokens);

                Runnable listener = mock(Runnable.class);
                combined.addReleaseListener(listener);

                assertFalse(combined.isReleased());
                verifyZeroInteractions(listener);

                subTokens[releaseIndex].release();

                assertTrue(combined.isReleased());
                verify(listener).run();

                // subsequent releases have no effect.
                for (int i = 0; i < numberOfTokens; i++) {
                    subTokens[i].release();
                    assertTrue(combined.isReleased());
                }
                combined.release();

                verifyNoMoreInteractions(listener);
            }
        }
    }

    @Test
    public void testReleaseOfCombinedReleasesSubtokens() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] subTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < subTokens.length; i++) {
                DelegatedAccessToken<String> subToken = new DelegatedAccessToken<>(
                        AccessTokens.createToken("TOKEN-" + CURRENT_INDEX.getAndIncrement()));
                subTokens[i] = spy(subToken);
            }

            CombinedToken<String> combined = create(subTokens);
            combined.release();

            for (int i = 0; i < subTokens.length; i++) {
                verify(subTokens[i]).release();
            }
        }
    }

    @Test
    public void testReleaseOfCombinedReleasesSubtokens2() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] subTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < subTokens.length; i++) {
                DelegatedAccessToken<String> subToken = new DelegatedAccessToken<>(
                        AccessTokens.createToken("TOKEN-" + CURRENT_INDEX.getAndIncrement()));
                subTokens[i] = spy(subToken);
            }

            CombinedToken<String> combined = create(subTokens);
            combined.releaseAndCancel();

            for (int i = 0; i < subTokens.length; i++) {
                verify(subTokens[i]).releaseAndCancel();
            }
        }
    }

    @Test
    public void testReleaseAndCancelCancelsTask() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int releaseIndex = 0; releaseIndex < numberOfTokens; releaseIndex++) {
                final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
                final CombinedToken<String> combined = create(subTokens);

                final AtomicBoolean initialNotCanceled = new AtomicBoolean(false);
                final AtomicBoolean postCanceled = new AtomicBoolean(false);

                final AccessToken<?> toRelease = subTokens[releaseIndex];
                TaskExecutor executor = combined.createExecutor(SyncTaskExecutor.getSimpleExecutor());
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    initialNotCanceled.set(!cancelToken.isCanceled());
                    toRelease.releaseAndCancel();
                    postCanceled.set(cancelToken.isCanceled());
                }, null);

                assertTrue(initialNotCanceled.get());
                assertTrue(postCanceled.get());
            }
        }
    }

    @Test
    public void testCancelBeforeExecute() throws Exception {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            final AccessToken<?>[] subTokens = createTokens(numberOfTokens);
            final CombinedToken<String> combined = create(subTokens);
            combined.release();

            TaskExecutor executor = combined.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

            verifyZeroInteractions(task);
            verify(cleanup).cleanup(eq(true), argThat(canceledOrNull()));
        }
    }

    @Test(timeout = 20000)
    public void testCanceledAwaitRelease() {
        final AccessToken<?>[] subTokens = createTokens(2);
        final CombinedToken<String> combined = create(subTokens);

        combined.release();
        combined.awaitRelease(Cancellation.UNCANCELABLE_TOKEN);
    }


    @Test(expected = OperationCanceledException.class, timeout = 20000)
    public void testCanceledTryAwaitRelease() {
        final AccessToken<?>[] subTokens = createTokens(2);
        final CombinedToken<String> combined = create(subTokens);

        combined.tryAwaitRelease(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @Test(timeout = 20000)
    public void testTryAwaitRelease() {
        for (int testIndex = 0; testIndex < 100; testIndex++) {
            AccessToken<?>[] subTokens = createTokens(2);
            CombinedToken<String> combined = create(subTokens);

            Tasks.runConcurrently(combined::release, () -> {
                if (!combined.tryAwaitRelease(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS)) {
                    throw new OperationCanceledException("timeout");
                }
            });
        }
    }

    private static ArgumentMatcher<Throwable> canceledOrNull() {
        return new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                if (argument instanceof OperationCanceledException) {
                    return true;
                }
                return argument == null;
            }
        };
    }
}
