package org.jtrim2.event;

import java.util.Arrays;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

public class ListenerRegistriesTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ListenerRegistries.class);
    }

    @Test
    public void combineListenerRefs() {
        MultiListenerRefTest.Combiner combiner = ListenerRegistries::combineListenerRefs;

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }

    @Test
    public void combineListenerRefsFromList() {
        MultiListenerRefTest.Combiner combiner = (ListenerRef[] refs) -> {
            return ListenerRegistries.combineListenerRefs(Arrays.asList(refs));
        };

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }
}
