package org.jtrim2.property.swing;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import org.jtrim2.property.BoolPropertyListener;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ComponentDisablerTest {
    private static Component[] createTestArray(int size) {
        Component[] result = new Component[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = mock(Component.class);
        }
        return result;
    }

    public static void testStateChanges1(ComponentDisablerFactory factory) {
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            for (boolean initialState: Arrays.asList(false, true)) {
                TestComponents components = new TestComponents(numberOfComponents, initialState);
                BoolPropertyListener disabler = factory.create(components.getComponents());
                components.checkStates(initialState);

                disabler.onChangeValue(true);
                components.checkStates(true);

                disabler.onChangeValue(false);
                components.checkStates(false);

                disabler.onChangeValue(false);
                components.checkStates(false);

                disabler.onChangeValue(true);
                components.checkStates(true);
            }
        }
    }

    @Test
    public void testStateChanges1() {
        testStateChanges1(Factory.INSTANCE);
    }

    public static void testStateChanges2(ComponentDisablerFactory factory) {
        for (int numberOfComponents: Arrays.asList(0, 1, 5)) {
            for (boolean initialState: Arrays.asList(false, true)) {
                TestComponents components = new TestComponents(numberOfComponents, initialState);
                BoolPropertyListener disabler = factory.create(components.getComponents());
                components.checkStates(initialState);

                disabler.onChangeValue(true);
                components.checkStates(true);

                disabler.onChangeValue(false);
                components.checkStates(false);

                disabler.onChangeValue(false);
                components.checkStates(false);

                disabler.onChangeValue(true);
                components.checkStates(true);
            }
        }
    }

    @Test
    public void testStateChanges2() {
        testStateChanges2(Factory.INSTANCE);
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
                doAnswer((InvocationOnMock invocation) -> {
                    states[componentIndex] = (boolean)invocation.getArguments()[0];
                    return null;
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

    private enum Factory implements ComponentDisablerFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(Component[] components) {
            return new ComponentDisabler(components);
        }

        @Override
        public BoolPropertyListener create(List<? extends Component> components) {
            return new ComponentDisabler(components.toArray(new Component[0]));
        }
    }
}
