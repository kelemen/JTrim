package org.jtrim2.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.jtrim2.collections.ArraysEx;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccessRequestTest {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final List<Object> EMPTY_LIST = Collections.emptyList();

    private static Object[] createObjArray(int length) {
        Object[] result = new Object[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Object();
        }
        return result;
    }

    private static List<Object> createObjList(int length) {
        return ArraysEx.viewAsList(createObjArray(length));
    }

    private static AccessRequest<Object, Object> createFromArray(Object id) {
        return new AccessRequest<>(id, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    private static AccessRequest<Object, Object> createFromList(Object id) {
        return new AccessRequest<>(id, EMPTY_LIST, EMPTY_LIST);
    }

    @Test(expected = NullPointerException.class)
    public void testNullId1() {
        createFromArray(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullId2() {
        createFromList(null);
    }

    @Test
    public void testNullReadRights1() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), null, createObjArray(1));
        assertTrue(request.getReadRights().isEmpty());
    }

    @Test
    public void testNullReadRights2() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), null, createObjList(1));
        assertTrue(request.getReadRights().isEmpty());
    }

    @Test
    public void testNullWriteRights1() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), createObjArray(1), null);
        assertTrue(request.getWriteRights().isEmpty());
    }

    @Test
    public void testNullWriteRights2() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), createObjList(1), null);
        assertTrue(request.getWriteRights().isEmpty());
    }

    @Test
    public void testGetReadRequest() {
        Object id = new Object();
        Object right = new Object();
        AccessRequest<Object, Object> request = AccessRequest.getReadRequest(id, right);

        assertSame(id, request.getRequestID());
        assertEquals(Collections.singleton(right), new HashSet<>(request.getReadRights()));
        assertTrue(request.getWriteRights().isEmpty());
    }

    @Test
    public void testGetWriteRequest() {
        Object id = new Object();
        Object right = new Object();
        AccessRequest<Object, Object> request = AccessRequest.getWriteRequest(id, right);

        assertSame(id, request.getRequestID());
        assertEquals(Collections.singleton(right), new HashSet<>(request.getWriteRights()));
        assertTrue(request.getReadRights().isEmpty());
    }

    @Test
    public void testGetReadRights1() {
        for (int elementCount = 0; elementCount < 5; elementCount++) {
            Object[] readRights = createObjArray(elementCount);
            AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), readRights, EMPTY_ARRAY);

            assertEquals(new HashSet<>(Arrays.asList(readRights)), new HashSet<>(request.getReadRights()));
        }
    }

    @Test
    public void testGetReadRights2() {
        for (int elementCount = 0; elementCount < 5; elementCount++) {
            List<Object> readRights = createObjList(elementCount);
            AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), readRights, EMPTY_LIST);

            assertEquals(new HashSet<>(readRights), new HashSet<>(request.getReadRights()));
        }
    }

    @Test
    public void testGetWriteRights1() {
        for (int elementCount = 0; elementCount < 5; elementCount++) {
            Object[] writeRights = createObjArray(elementCount);
            AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), EMPTY_ARRAY, writeRights);

            assertEquals(new HashSet<>(Arrays.asList(writeRights)), new HashSet<>(request.getWriteRights()));
        }
    }

    @Test
    public void testGetWriteRights2() {
        for (int elementCount = 0; elementCount < 5; elementCount++) {
            List<Object> writeRights = createObjList(elementCount);
            AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), EMPTY_LIST, writeRights);

            assertEquals(new HashSet<>(writeRights), new HashSet<>(request.getWriteRights()));
        }
    }

    @Test
    public void testGetRequestID1() {
        Object id = new Object();
        AccessRequest<Object, Object> request = new AccessRequest<>(id, EMPTY_ARRAY, EMPTY_ARRAY);
        assertSame(id, request.getRequestID());
    }

    @Test
    public void testGetRequestID2() {
        Object id = new Object();
        AccessRequest<Object, Object> request = new AccessRequest<>(id, EMPTY_LIST, EMPTY_LIST);
        assertSame(id, request.getRequestID());
    }

    @Test
    public void testToString1() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), createObjArray(2), EMPTY_ARRAY);
        assertNotNull(request.toString());
    }

    @Test
    public void testToString2() {
        AccessRequest<Object, Object> request = new AccessRequest<>(new Object(), EMPTY_LIST, createObjList(2));
        assertNotNull(request.toString());
    }
}
