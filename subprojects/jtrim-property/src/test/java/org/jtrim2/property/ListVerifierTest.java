package org.jtrim2.property;

import java.util.Arrays;
import java.util.List;
import org.jtrim2.collections.ArraysEx;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ListVerifierTest {
    private static final String VERIFIED_SUFFIX = ".VERIFIED";

    @SuppressWarnings("unchecked")
    private static <T> PropertyVerifier<T> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    private static PropertyVerifier<String> stubbedVerifier() {
        PropertyVerifier<String> verifier = mockVerifier();
        when(verifier.storeValue(any())).thenAnswer((InvocationOnMock invocation) -> {
            Object arg = invocation.getArguments()[0];
            return arg + VERIFIED_SUFFIX;
        });
        return verifier;
    }

    private void testNotNullElements(int elementCount, boolean allowNull) {
        PropertyVerifier<String> elementVerifier = stubbedVerifier();
        ListVerifier<String> verifier = new ListVerifier<>(elementVerifier, allowNull);

        String[] input = new String[elementCount];
        String[] output = new String[elementCount];
        for (int i = 0; i < elementCount; i++) {
            input[i] = "ListVerifierTest-32746835" + "." + i;
            output[i] = input[i] + VERIFIED_SUFFIX;
        }

        verifyNoInteractions(elementVerifier);
        List<String> verified = verifier.storeValue(ArraysEx.viewAsList(input));

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(elementVerifier, times(elementCount)).storeValue(argCaptor.capture());

        assertEquals(Arrays.asList(input), argCaptor.getAllValues());
        assertEquals(Arrays.asList(output), verified);
    }

    private void testNullElements(int elementCount, boolean allowNull) {
        PropertyVerifier<String> elementVerifier = stubbedVerifier();
        ListVerifier<String> verifier = new ListVerifier<>(elementVerifier, allowNull);

        String[] input = new String[elementCount];
        String[] output = new String[elementCount];
        for (int i = 0; i < elementCount; i++) {
            input[i] = null;
            output[i] = input[i] + VERIFIED_SUFFIX;
        }

        verifyNoInteractions(elementVerifier);
        List<String> verified = verifier.storeValue(ArraysEx.viewAsList(input));

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(elementVerifier, times(elementCount)).storeValue(argCaptor.capture());

        assertEquals(Arrays.asList(input), argCaptor.getAllValues());
        assertEquals(Arrays.asList(output), verified);
    }

    @Test
    public void testNotNullElements() {
        for (boolean allowNull: Arrays.asList(false, true)) {
            for (int elementCount = 0; elementCount < 5; elementCount++) {
                testNotNullElements(elementCount, allowNull);
            }
        }
    }

    @Test
    public void testNullElements() {
        for (boolean allowNull: Arrays.asList(false, true)) {
            for (int elementCount = 0; elementCount < 5; elementCount++) {
                testNullElements(elementCount, allowNull);
            }
        }
    }

    @Test
    public void testNullList() {
        PropertyVerifier<String> elementVerifier = stubbedVerifier();
        ListVerifier<String> verifier = new ListVerifier<>(elementVerifier, true);

        List<String> verified = verifier.storeValue(null);
        assertNull(verified);
        verifyNoInteractions(elementVerifier);
    }

    @Test(expected = NullPointerException.class)
    public void testNullListNotAllowsNullList() {
        PropertyVerifier<String> elementVerifier = stubbedVerifier();
        ListVerifier<String> verifier = new ListVerifier<>(elementVerifier, false);
        verifier.storeValue(null);
    }
}
