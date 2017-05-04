package org.jtrim2.swing.access;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.property.swing.ComponentDisablerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.swing.access.CompatibilityUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("deprecation")
public class ComponentDisablerTest {
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

    private static ComponentDisabler createFromArray(Component[] components) {
        return new ComponentDisabler(components);
    }

    private static ComponentDisabler createFromList(Component[] components) {
        return new ComponentDisabler(Arrays.asList(components));
    }

    private static Component[] createTestArray(int size) {
        Component[] result = new Component[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = mock(Component.class);
        }
        return result;
    }

    private static <E> Set<E> identitySet(E[] elements) {
        Set<E> result = CollectionsEx.newIdentityHashSet(elements.length);
        result.addAll(Arrays.asList(elements));
        return result;
    }

    private static <E> Set<E> identitySet(Collection<? extends E> elements) {
        Set<E> result = CollectionsEx.newIdentityHashSet(elements.size());
        result.addAll(elements);
        return result;
    }

    @Test
    public void testGetComponents1() {
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            Component[] components = createTestArray(numberOfComponents);
            ComponentDisabler disabler = createFromArray(components);
            assertEquals(identitySet(components), identitySet(disabler.getComponents()));
        }
    }

    @Test
    public void testGetComponents2() {
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            Component[] components = createTestArray(numberOfComponents);
            ComponentDisabler disabler = createFromList(components);
            assertEquals(identitySet(components), identitySet(disabler.getComponents()));
        }
    }

    @Test
    public void testStateChanges1() {
        org.jtrim2.property.swing.ComponentDisablerTest.testStateChanges1(Factory.INSTANCE);
    }

    @Test
    public void testStateChanges2() {
        org.jtrim2.property.swing.ComponentDisablerTest.testStateChanges2(Factory.INSTANCE);
    }

    private enum Factory implements ComponentDisablerFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(Component[] components) {
            return toBoolPropertyListener(new ComponentDisabler(components));
        }

        @Override
        public BoolPropertyListener create(List<? extends Component> components) {
            return toBoolPropertyListener(new ComponentDisabler(components));
        }
    }
}
