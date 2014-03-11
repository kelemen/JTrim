package org.jtrim.event;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class ListenerRegistriesTest {
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
    public void combineListenerRefs() {
        MultiListenerRefTest.Combiner combiner = new MultiListenerRefTest.Combiner() {
            @Override
            public ListenerRef combine(ListenerRef[] refs) {
                return ListenerRegistries.combineListenerRefs(refs);
            }
        };

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }

    @Test
    public void combineListenerRefsFromList() {
        MultiListenerRefTest.Combiner combiner = new MultiListenerRefTest.Combiner() {
            @Override
            public ListenerRef combine(ListenerRef[] refs) {
                return ListenerRegistries.combineListenerRefs(Arrays.asList(refs));
            }
        };

        MultiListenerRefTest.testMultiple(1, combiner);
        MultiListenerRefTest.testMultiple(2, combiner);
        MultiListenerRefTest.testMultiple(3, combiner);
    }
}
