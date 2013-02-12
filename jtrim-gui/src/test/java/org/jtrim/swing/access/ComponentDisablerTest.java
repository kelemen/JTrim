package org.jtrim.swing.access;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.jtrim.collections.CollectionsEx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
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
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            for (boolean initialState: Arrays.asList(false, true)) {
                TestComponents components = new TestComponents(numberOfComponents, initialState);
                ComponentDisabler disabler = createFromArray(components.getComponents());
                components.checkStates(initialState);

                disabler.onChangeAccess(true);
                components.checkStates(true);

                disabler.onChangeAccess(false);
                components.checkStates(false);

                disabler.onChangeAccess(false);
                components.checkStates(false);

                disabler.onChangeAccess(true);
                components.checkStates(true);
            }
        }
    }

    @Test
    public void testStateChanges2() {
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            for (boolean initialState: Arrays.asList(false, true)) {
                TestComponents components = new TestComponents(numberOfComponents, initialState);
                ComponentDisabler disabler = createFromList(components.getComponents());
                components.checkStates(initialState);

                disabler.onChangeAccess(true);
                components.checkStates(true);

                disabler.onChangeAccess(false);
                components.checkStates(false);

                disabler.onChangeAccess(false);
                components.checkStates(false);

                disabler.onChangeAccess(true);
                components.checkStates(true);
            }
        }
    }

    private static class TestComponents {
        private final boolean[] states;
        private final Component[] components;

        public TestComponents(int numberOfComponents, boolean initialState) {
            this.states = new boolean[numberOfComponents];
            this.components = createTestArray(numberOfComponents);

            for (int i = 0; i < numberOfComponents; i++) {
                final int componentIndex = i;

                states[i] = initialState;
                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        states[componentIndex] = (boolean)invocation.getArguments()[0];
                        return null;
                    }
                }).when(components[i]).setEnabled(anyBoolean());
            }
        }

        public Component[] getComponents() {
            return components.clone();
        }

        public void checkStates(boolean expected) {
            for (boolean state: states) {
                assertEquals(expected, state);
            }
        }
    }
}