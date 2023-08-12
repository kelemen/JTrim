package org.jtrim2.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccessManagersTest {
    @SuppressWarnings("unchecked")
    private static <IDType, RightType> AccessManager<IDType, RightType> mockManager() {
        return mock(AccessManager.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<AccessRequest<?, ?>> argRequestCapture() {
        return (ArgumentCaptor<AccessRequest<?, ?>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(AccessRequest.class);
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AccessManagers.class);
    }

    /**
     * Test of tryGetReadAccess method, of class AccessManagers.
     */
    @Test
    public void testTryGetReadAccess() {
        AccessManager<Object, Object> manager = mockManager();

        AccessResult<Object> expectedResult = new AccessResult<>(AccessTokens.createToken(new Object()));
        when(manager.tryGetAccess((AccessRequest<?, ?>) any(AccessRequest.class)))
                .thenReturn(expectedResult);

        Object requestID = new Object();
        Collection<Object> rights = Arrays.asList(new Object(), new Object());
        AccessResult<Object> result = AccessManagers.tryGetReadAccess(manager, requestID, rights);

        assertSame(expectedResult, result);

        ArgumentCaptor<AccessRequest<?, ?>> argRequest = argRequestCapture();
        verify(manager).tryGetAccess(argRequest.capture());

        AccessRequest<?, ?> request = argRequest.getValue();
        assertEquals(new HashSet<>(rights), new HashSet<>(request.getReadRights()));
        assertTrue(request.getWriteRights().isEmpty());
    }

    /**
     * Test of tryGetWriteAccess method, of class AccessManagers.
     */
    @Test
    public void testTryGetWriteAccess() {
        AccessManager<Object, Object> manager = mockManager();

        AccessResult<Object> expectedResult = new AccessResult<>(AccessTokens.createToken(new Object()));
        when(manager.tryGetAccess((AccessRequest<?, ?>) any(AccessRequest.class)))
                .thenReturn(expectedResult);

        Object requestID = new Object();
        Collection<Object> rights = Arrays.asList(new Object(), new Object());
        AccessResult<Object> result = AccessManagers.tryGetWriteAccess(manager, requestID, rights);

        assertSame(expectedResult, result);

        ArgumentCaptor<AccessRequest<?, ?>> argRequest = argRequestCapture();
        verify(manager).tryGetAccess(argRequest.capture());

        AccessRequest<?, ?> request = argRequest.getValue();
        assertEquals(new HashSet<>(rights), new HashSet<>(request.getWriteRights()));
        assertTrue(request.getReadRights().isEmpty());
    }

    /**
     * Test of getScheduledReadAccess method, of class AccessManagers.
     */
    @Test
    public void testGetScheduledReadAccess() {
        AccessManager<Object, Object> manager = mockManager();

        AccessResult<Object> expectedResult = new AccessResult<>(AccessTokens.createToken(new Object()));
        when(manager.getScheduledAccess((AccessRequest<?, ?>) any(AccessRequest.class)))
                .thenReturn(expectedResult);

        Object requestID = new Object();
        Collection<Object> rights = Arrays.asList(new Object(), new Object());
        AccessResult<Object> result = AccessManagers.getScheduledReadAccess(manager, requestID, rights);

        assertSame(expectedResult, result);

        ArgumentCaptor<AccessRequest<?, ?>> argRequest = argRequestCapture();
        verify(manager).getScheduledAccess(argRequest.capture());

        AccessRequest<?, ?> request = argRequest.getValue();
        assertEquals(new HashSet<>(rights), new HashSet<>(request.getReadRights()));
        assertTrue(request.getWriteRights().isEmpty());
    }

    /**
     * Test of getScheduledWriteAccess method, of class AccessManagers.
     */
    @Test
    public void testGetScheduledWriteAccess() {
        AccessManager<Object, Object> manager = mockManager();

        AccessResult<Object> expectedResult = new AccessResult<>(AccessTokens.createToken(new Object()));
        when(manager.getScheduledAccess((AccessRequest<?, ?>) any(AccessRequest.class)))
                .thenReturn(expectedResult);

        Object requestID = new Object();
        Collection<Object> rights = Arrays.asList(new Object(), new Object());
        AccessResult<Object> result = AccessManagers.getScheduledWriteAccess(manager, requestID, rights);

        assertSame(expectedResult, result);

        ArgumentCaptor<AccessRequest<?, ?>> argRequest = argRequestCapture();
        verify(manager).getScheduledAccess(argRequest.capture());

        AccessRequest<?, ?> request = argRequest.getValue();
        assertEquals(new HashSet<>(rights), new HashSet<>(request.getWriteRights()));
        assertTrue(request.getReadRights().isEmpty());
    }
}
