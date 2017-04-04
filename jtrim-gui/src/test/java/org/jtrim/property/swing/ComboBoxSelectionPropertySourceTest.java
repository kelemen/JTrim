package org.jtrim.property.swing;

import javax.swing.JComboBox;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.component.GuiTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ComboBoxSelectionPropertySourceTest {
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

    private void testValuePropertyOnEdt() {
        JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        comboBox.setSelectedItem(1);

        PropertySource<Integer> property = ComboBoxSelectionPropertySource.createProperty(comboBox);

        assertEquals(1, property.getValue().intValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        comboBox.setSelectedItem(2);

        verify(listener, atLeastOnce()).run();

        assertEquals(2, property.getValue().intValue());

        listenerRef.unregister();
        comboBox.setSelectedItem(3);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testStandardProperties() {
        GuiTestUtils.runOnEDT(this::testValuePropertyOnEdt);
    }

    @Test
    public void testNullComponent() {
        GuiTestUtils.runOnEDT(() -> {
            try {
                SpinnerValuePropertySource.createProperty(null);
                fail("Expected NullPointerException");
            } catch (NullPointerException ex) {
            }
        });
    }

    @Test
    public void testSetValue() {
        GuiTestUtils.runOnEDT(() -> {
            JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
            comboBox.setSelectedItem(1);

            MutableProperty<Integer> property = ComboBoxSelectionPropertySource.createProperty(comboBox);
            property.setValue(2);
            assertEquals(2, property.getValue().intValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);
            verifyZeroInteractions(listener);

            property.setValue(3);
            verify(listener, atLeastOnce()).run();

            assertEquals(3, property.getValue().intValue());
        });
    }

    @Test
    public void testSetValueToNonExisting() {
        GuiTestUtils.runOnEDT(() -> {
            JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
            comboBox.setSelectedItem(1);

            MutableProperty<Integer> property = ComboBoxSelectionPropertySource.createProperty(comboBox);
            property.setValue(100);
            assertEquals(1, property.getValue().intValue());
        });
    }

    @Test
    public void testSetValueToNull() {
        GuiTestUtils.runOnEDT(() -> {
            JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
            comboBox.setSelectedItem(1);

            MutableProperty<Integer> property = ComboBoxSelectionPropertySource.createProperty(comboBox);
            property.setValue(null);
            assertNull(property.getValue());
        });
    }
}
