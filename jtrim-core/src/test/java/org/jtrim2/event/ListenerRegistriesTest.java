package org.jtrim2.event;

import java.util.Arrays;
import org.jtrim2.utils.TestUtils;
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
