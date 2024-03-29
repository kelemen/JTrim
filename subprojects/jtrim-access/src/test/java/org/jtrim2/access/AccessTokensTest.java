package org.jtrim2.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccessTokensTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AccessTokens.class);
    }

    @Test
    public void testAddReleaseAllListener() {
        for (int numberOfTokens = 0; numberOfTokens < 5; numberOfTokens++) {
            AccessToken<?>[] accessTokens = new AccessToken<?>[numberOfTokens];
            for (int i = 0; i < accessTokens.length; i++) {
                accessTokens[i] = AccessTokens.createToken("TOKEN-" + i);
            }

            Runnable listener = mock(Runnable.class);
            AccessTokens.addReleaseAllListener(Arrays.asList(accessTokens), listener);
            for (int i = 0; i < numberOfTokens; i++) {
                verifyNoInteractions(listener);
                accessTokens[i].release();
            }
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

                when(accessTokens[i].addReleaseListener(any(Runnable.class))).thenReturn(wrappedRef);
                when(accessTokens[i].isReleased()).thenReturn(false);
            }


            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef
                    = AccessTokens.addReleaseAllListener(Arrays.asList(accessTokens), listener);
            listenerRef.unregister();

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
                AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);

                verifyNoInteractions(listener);

                accessTokens[canceledIndex].release();

                verify(listener).run();

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
                AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);

                verify(listener).run();
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

                    when(accessTokens[i].addReleaseListener(any(Runnable.class))).thenReturn(wrappedRef);
                    when(accessTokens[i].isReleased()).thenReturn(false);
                }

                Runnable listener = mock(Runnable.class);
                ListenerRef listenerRef
                        = AccessTokens.addReleaseAnyListener(Arrays.asList(accessTokens), listener);
                listenerRef.unregister();

                accessTokens[canceledIndex].release();

                verifyNoInteractions(listener);

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
        executor.execute(() -> {
            inContext1.set(token1.isExecutingInThis());
            inContext2.set(token2.isExecutingInThis());
        });

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
