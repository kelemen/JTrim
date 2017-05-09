package org.jtrim2.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CombinedVerifierTest {
    @SuppressWarnings("unchecked")
    private static PropertyVerifier<Object> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    private static CombinedVerifier<Object> create(List<PropertyVerifier<Object>> verifiers) {
        return new CombinedVerifier<>(verifiers);
    }

    @Test
    public void testZeroVerifier() {
        CombinedVerifier<Object> combined = create(Collections.<PropertyVerifier<Object>>emptyList());

        Object input = new Object();
        assertSame(input, combined.storeValue(input));
        assertNull(combined.storeValue(null));
    }

    private void testVerifier(int verifierCount) {
        assert verifierCount > 0;

        List<PropertyVerifier<Object>> verifiers = new ArrayList<>(verifierCount);
        Object[] args = new Object[verifierCount + 1];
        for (int i = 0; i < args.length; i++) {
            args[i] = new Object();
        }

        for (int i = 0; i < verifierCount; i++) {
            PropertyVerifier<Object> verifier = mockVerifier();
            stub(verifier.storeValue(same(args[i]))).toReturn(args[i + 1]);

            verifiers.add(verifier);
        }

        CombinedVerifier<Object> combined = create(verifiers);

        assertSame("Invalid result using " + verifierCount + " verifiers",
                args[args.length - 1],
                combined.storeValue(args[0]));
    }

    @Test
    public void testMoreVerifier() {
        for (int verifierCount = 1; verifierCount < 5; verifierCount++) {
            testVerifier(verifierCount);
        }
    }
}
