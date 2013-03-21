package org.jtrim.property;

import java.util.Arrays;
import java.util.List;
import org.jtrim.collections.ArraysEx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ListVerifierTest {
    private static final String VERIFIED_SUFFIX = ".VERIFIED";

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

    @SuppressWarnings("unchecked")
    private static <T> PropertyVerifier<T> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    private static PropertyVerifier<String> stubbedVerifier() {
        PropertyVerifier<String> verifier = mockVerifier();
        stub(verifier.storeValue(any(String.class))).toAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                Object arg = invocation.getArguments()[0];
                return arg + VERIFIED_SUFFIX;
            }
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

        verifyZeroInteractions(elementVerifier);
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

        verifyZeroInteractions(elementVerifier);
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
        verifyZeroInteractions(elementVerifier);
    }

    @Test(expected = NullPointerException.class)
    public void testNullListNotAllowsNullList() {
        PropertyVerifier<String> elementVerifier = stubbedVerifier();
        ListVerifier<String> verifier = new ListVerifier<>(elementVerifier, false);
        verifier.storeValue(null);
    }
}
