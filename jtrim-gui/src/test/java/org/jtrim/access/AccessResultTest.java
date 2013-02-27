package org.jtrim.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AccessResultTest {
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

    private static <T> AccessResult<T> create(AccessToken<T> accessToken) {
        return new AccessResult<>(accessToken);
    }

    private static <T> AccessResult<T> create(Collection<? extends AccessToken<T>> blockingTokens) {
        return new AccessResult<>(blockingTokens);
    }

    private static <T> AccessResult<T> create(AccessToken<T> accessToken,
            Collection<? extends AccessToken<T>> blockingTokens) {
        return new AccessResult<>(accessToken, blockingTokens);
    }

    @SuppressWarnings("unchecked")
    private static <T> AccessToken<T> mockToken() {
        return mock(AccessToken.class);
    }

    @Test
    public void allowNullBlockingCollection() {
        Collection<AccessToken<Object>> blockingTokens = null;
        AccessResult<Object> result = create(blockingTokens);

        assertTrue(result.getBlockingTokens().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void nullBlockingTokenIsNotAllowed1() {
        create(Arrays.asList(null, mockToken()));
    }

    @Test(expected = NullPointerException.class)
    public void nullBlockingTokenIsNotAllowed2() {
        create(Arrays.asList(mockToken(), null));
    }

    /**
     * Test of release method, of class AccessResult.
     */
    @Test
    public void testRelease() {
        AccessToken<Object> token = mockToken();
        AccessResult<Object> result = create(token);

        result.release();

        verify(token).release();
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testReleaseNoToken() {
        AccessToken<Object> token1 = mockToken();
        AccessToken<Object> token2 = mockToken();
        AccessResult<Object> result = create(Arrays.asList(token1, token2));

        result.release();

        verifyZeroInteractions(token1, token2);
    }

    /**
     * Test of releaseAndCancel method, of class AccessResult.
     */
    @Test
    public void testReleaseAndCancel() {
        AccessToken<Object> token = mockToken();
        AccessResult<Object> result = create(token);

        result.releaseAndCancel();

        verify(token).releaseAndCancel();
        verifyNoMoreInteractions(token);
    }

    @Test
    public void testReleaseAndCancelNoToken() {
        AccessToken<Object> token1 = mockToken();
        AccessToken<Object> token2 = mockToken();
        AccessResult<Object> result = create(Arrays.asList(token1, token2));

        result.releaseAndCancel();

        verifyZeroInteractions(token1, token2);
    }

    /**
     * Test of isAvailable method, of class AccessResult.
     */
    @Test
    public void testIsAvailableTrue() {
        AccessResult<Object> result = create(mockToken());
        assertTrue(result.isAvailable());
    }

    @Test
    public void testIsAvailableFalse() {
        AccessResult<Object> result = create(Arrays.asList(mockToken(), mockToken()));
        assertFalse(result.isAvailable());
    }

    /**
     * Test of getBlockingIDs method, of class AccessResult.
     */
    @Test
    public void testGetBlockingIDs() {
        AccessToken<Object> token1 = mockToken();
        AccessToken<Object> token2 = mockToken();
        List<AccessToken<Object>> blockingTokens = Arrays.asList(token1, token2);

        Object id1 = new Object();
        Object id2 = new Object();

        stub(token1.getAccessID()).toReturn(id1);
        stub(token2.getAccessID()).toReturn(id2);

        AccessResult<Object> result = create(blockingTokens);

        // Read twice because the result is calculated and cached lazily.
        assertEquals(result.getBlockingIDs(), new HashSet<>(Arrays.asList(id1, id2)));
        assertEquals(result.getBlockingIDs(), new HashSet<>(Arrays.asList(id1, id2)));

        // Check if it does return the same set because otherwise it would be
        // a waste of memory.
        assertSame(result.getBlockingIDs(), result.getBlockingIDs());
    }

    @Test
    public void testGetBlockingIDsNoBlocking() {
        AccessResult<Object> result = create(mockToken());
        assertTrue(result.getBlockingIDs().isEmpty());
    }

    /**
     * Test of getAccessToken method, of class AccessResult.
     */
    @Test
    public void testGetAccessToken1() {
        AccessToken<Object> token = mockToken();
        AccessResult<Object> result = create(token);

        assertSame(token, result.getAccessToken());
    }

    @Test
    public void testGetAccessToken2() {
        AccessToken<Object> token = mockToken();
        AccessResult<Object> result = create(token, Arrays.asList(mockToken(), mockToken()));

        assertSame(token, result.getAccessToken());
    }


    @Test
    public void testGetAccessTokenNoToken() {
        AccessResult<Object> result = create(Arrays.asList(mockToken(), mockToken()));

        assertNull(result.getAccessToken());
    }

    /**
     * Test of getBlockingTokens method, of class AccessResult.
     */
    @Test
    public void testGetBlockingTokens() {
        AccessToken<Object> token1 = mockToken();
        AccessToken<Object> token2 = mockToken();
        List<AccessToken<Object>> blockingTokens = Arrays.asList(token1, token2);

        AccessResult<Object> result = create(blockingTokens);
        assertEquals(new HashSet<>(blockingTokens), new HashSet<>(result.getBlockingTokens()));
    }

    @Test
    public void testGetBlockingTokensNoBlocking() {
        AccessResult<Object> result = create(mockToken());
        assertTrue(result.getBlockingTokens().isEmpty());
    }

    /**
     * Test of releaseAndCancelBlockingTokens method, of class AccessResult.
     */
    @Test
    public void testReleaseAndCancelBlockingTokens() {
        AccessToken<Object> token1 = mockToken();
        AccessToken<Object> token2 = mockToken();
        List<AccessToken<Object>> blockingTokens = Arrays.asList(token1, token2);

        AccessResult<Object> result = create(blockingTokens);

        result.releaseAndCancelBlockingTokens();

        verify(token1).releaseAndCancel();
        verify(token2).releaseAndCancel();
        verifyNoMoreInteractions(token1, token2);
    }

    @Test
    public void testReleaseAndCancelBlockingTokensNoBlocking() {
        AccessToken<Object> token = mockToken();

        AccessResult<Object> result = create(token);

        result.releaseAndCancelBlockingTokens();

        verifyZeroInteractions(token);
    }

    /**
     * Test of toString method, of class AccessResult.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockToken()).toString());
        assertNotNull(create(Arrays.asList(mockToken(), mockToken())).toString());
        assertNotNull(create(mockToken(), Arrays.asList(mockToken(), mockToken())).toString());
    }
}
