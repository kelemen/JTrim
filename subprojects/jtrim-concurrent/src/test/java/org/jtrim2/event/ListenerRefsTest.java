package org.jtrim2.event;

import java.util.Arrays;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

public class ListenerRefsTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ListenerRefs.class);
    }

    @Test
    public void combineListenerRefs() {
        MultiListenerRefTest.Combiner combiner = ListenerRefs::combineListenerRefs;

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }

    @Test
    public void combineListenerRefsFromList() {
        MultiListenerRefTest.Combiner combiner = (ListenerRef[] refs) -> {
            return ListenerRefs.combineListenerRefs(Arrays.asList(refs));
        };

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }
}
