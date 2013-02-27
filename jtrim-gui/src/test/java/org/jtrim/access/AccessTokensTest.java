package org.jtrim.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AccessTokensTest {
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

    @Test
    public void testAddReleaseAllListener() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < accessTokens.length; i++) {
                accessTokens[i] = AccessTokens.createToken("TOKEN-" + i);
            }

            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef = AccessTokens.addReleaseAllListener(Arrays.asList(accessTokens), listener);
            for (int i = 0; i < numberOfTokens; i++) {
                verifyZeroInteractions(listener);
                assertTrue(listenerRef.isRegistered());
                accessTokens[i].release();
            }
            assertFalse(listenerRef.isRegistered());
            verify(listener).run();
        }
    }

    @Test
    public void testAddReleaseAllListenerUnregister() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            ListenerRef[] wrappedRefs = new ListenerRef[numberOfTokens];
            for (int i = 0; i < accessTokens.length; i++) {
                final ListenerRef wrappedRef = mock(ListenerRef.class);

                accessTokens[i] = mock(AccessToken.class);
                wrappedRefs[i] = wrappedRef;

                stub(accessTokens[i].addReleaseListener(any(Runnable.class))).toReturn(wrappedRef);
                stub(accessTokens[i].isReleased()).toReturn(false);
                stub(wrappedRefs[i].isRegistered()).toReturn(false);
            }


            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef
                    = AccessTokens.addReleaseAllListener(Arrays.asList(accessTokens), listener);
            listenerRef.unregister();
            assertFalse(listenerRef.isRegistered());

            for (int i = 0; i < numberOfTokens; i++) {
                verify(wrappedRefs[i], atLeastOnce()).unregister();
            }
        }
    }

    @Test
    public void testAddReleaseAnyListener() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int canceledIndex = 0; canceledIndex < numberOfTokens; canceledIndex++) {
                AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
                for (int i = 0; i < accessTokens.length; i++) {
                    accessTokens[i] = AccessTokens.createToken("TOKEN-" + i);
                }

                Runnable listener = mock(Runnable.class);
                ListenerRef listenerRef
                        = AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);

                assertTrue(listenerRef.isRegistered());
                verifyZeroInteractions(listener);

                accessTokens[canceledIndex].release();

                verify(listener).run();
                assertFalse(listenerRef.isRegistered());

                for (int i = 0; i < accessTokens.length; i++) {
                    accessTokens[i].release();
                    verify(listener).run();
                }
            }
        }
    }

    @Test
    public void testAddReleaseAnyListenerAlreadyReleased() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int canceledIndex = 0; canceledIndex < numberOfTokens; canceledIndex++) {
                AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
                for (int i = 0; i < accessTokens.length; i++) {
                    accessTokens[i] = AccessTokens.createToken("TOKEN-" + i);
                }

                accessTokens[canceledIndex].release();

                Runnable listener = mock(Runnable.class);
                ListenerRef listenerRef
                        = AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);

                verify(listener).run();
                assertFalse(listenerRef.isRegistered());
            }
        }
    }

    @Test
    public void testAddReleaseAnyListenerUnregister() {
        for (int numberOfTokens = 1; numberOfTokens < 5; numberOfTokens++) {
            for (int canceledIndex = 0; canceledIndex < numberOfTokens; canceledIndex++) {
                AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
                ListenerRef[] wrappedRefs = new ListenerRef[numberOfTokens];
                for (int i = 0; i < accessTokens.length; i++) {
                    final ListenerRef wrappedRef = mock(ListenerRef.class);

                    accessTokens[i] = mock(AccessToken.class);
                    wrappedRefs[i] = wrappedRef;

                    stub(accessTokens[i].addReleaseListener(any(Runnable.class))).toReturn(wrappedRef);
                    stub(accessTokens[i].isReleased()).toReturn(false);
                    stub(wrappedRefs[i].isRegistered()).toReturn(false);
                }

                Runnable listener = mock(Runnable.class);
                ListenerRef listenerRef
                        = AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);
                listenerRef.unregister();

                accessTokens[canceledIndex].release();

                assertFalse(listenerRef.isRegistered());
                verifyZeroInteractions(listener);

                for (int i = 0; i < numberOfTokens; i++) {
                    verify(wrappedRefs[i], atLeastOnce()).unregister();
                }
            }
        }
    }

    /**
     * Test of createToken method, of class AccessTokens.
     */
    @Test
    public void testCreateToken() {
        Object id = new Object();
        AccessToken<Object> token = AccessTokens.createToken(id);
        assertTrue(token instanceof GenericAccessToken);
        assertSame(id, token.getAccessID());
    }

    /**
     * Test of combineTokens method, of class AccessTokens.
     */
    @Test
    public void testCombineTokens() {
        final AccessToken<String> token1 = AccessTokens.createToken("TOKEN-1");
        final AccessToken<String> token2 = AccessTokens.createToken("TOKEN-2");

        Object tokenID = new Object();
        AccessToken<Object> combined = AccessTokens.combineTokens(tokenID, token1, token2);
        assertTrue(combined instanceof CombinedToken);
        assertSame(tokenID, combined.getAccessID());

        TaskExecutor executor = combined.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContext1 = new AtomicBoolean(false);
        final AtomicBoolean inContext2 = new AtomicBoolean(false);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                inContext1.set(token1.isExecutingInThis());
                inContext2.set(token2.isExecutingInThis());
            }
        }, null);

        assertTrue(inContext1.get());
        assertTrue(inContext2.get());
    }

    /**
     * Test of releaseAndCancelTokens method, of class AccessTokens.
     */
    @Test
    public void testReleaseAndCancelTokens_AccessTokenArr() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < accessTokens.length; i++) {
                accessTokens[i] = mock(AccessToken.class);
            }

            AccessTokens.releaseAndCancelTokens(accessTokens);
            for (int i = 0; i < accessTokens.length; i++) {
                verify(accessTokens[i]).releaseAndCancel();
                verifyNoMoreInteractions(accessTokens[i]);
            }
        }
    }

    /**
     * Test of releaseAndCancelTokens method, of class AccessTokens.
     */
    @Test
    public void testReleaseAndCancelTokens_Collection() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < accessTokens.length; i++) {
                accessTokens[i] = mock(AccessToken.class);
            }

            AccessTokens.releaseAndCancelTokens(Arrays.asList(accessTokens));
            for (int i = 0; i < accessTokens.length; i++) {
                verify(accessTokens[i]).releaseAndCancel();
                verifyNoMoreInteractions(accessTokens[i]);
            }
        }
    }

    /**
     * Test of unblockResults method, of class AccessTokens.
     */
    @Test
    public void testUnblockResults_CancellationToken_AccessResultArr() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessResult<?>[] results = new AccessResult<?>[numberOfTokens];
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < results.length; i++) {
                accessTokens[i] = mock(AccessToken.class);
                results[i] = new AccessResult<>(Collections.singleton(accessTokens[i]));
            }

            CancellationToken cancelToken = mock(CancellationToken.class);
            AccessTokens.unblockResults(cancelToken, results);
            for (int i = 0; i < results.length; i++) {
                InOrder inOrder = inOrder(accessTokens[i]);
                inOrder.verify(accessTokens[i]).releaseAndCancel();
                inOrder.verify(accessTokens[i]).awaitRelease(same(cancelToken));
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    /**
     * Test of unblockResults method, of class AccessTokens.
     */
    @Test
    public void testUnblockResults_CancellationToken_Collection() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessResult<?>[] results = new AccessResult<?>[numberOfTokens];
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < results.length; i++) {
                accessTokens[i] = mock(AccessToken.class);
                results[i] = new AccessResult<>(Collections.singleton(accessTokens[i]));
            }

            CancellationToken cancelToken = mock(CancellationToken.class);
            AccessTokens.unblockResults(cancelToken, Arrays.asList(results));
            for (int i = 0; i < results.length; i++) {
                InOrder inOrder = inOrder(accessTokens[i]);
                inOrder.verify(accessTokens[i]).releaseAndCancel();
                inOrder.verify(accessTokens[i]).awaitRelease(same(cancelToken));
                inOrder.verifyNoMoreInteractions();
            }
        }
    }
}
