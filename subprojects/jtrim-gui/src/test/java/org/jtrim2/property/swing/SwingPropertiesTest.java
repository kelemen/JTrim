package org.jtrim2.property.swing;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import org.jtrim2.event.EventListeners;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.component.GuiTestUtils;
import org.jtrim2.testutils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SwingPropertiesTest {
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
        TestUtils.testUtilityClass(SwingProperties.class);
    }

    /**
     * Test of fromSwingSource method, of class SwingProperties.
     */
    @Test
    public void testFromSwingSource() {
        Object initialValue = new Object();
        TestSwingProperty wrapped = new TestSwingProperty(initialValue);
        PropertySource<Object> property
                = SwingProperties.fromSwingSource(wrapped, TestSwingProperty.listenerForwarder());

        assertTrue(property instanceof SwingBasedPropertySource);
        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener).run();

    }

    /**
     * Test of toSwingSource method, of class SwingProperties.
     */
    @Test
    public void testToSwingSource() {
        Object initialValue = new Object();
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(initialValue);
        SwingPropertySource<Object, Runnable> property
                = SwingProperties.toSwingSource(wrapped, EventListeners.runnableDispatcher());

        assertTrue(property instanceof StandardBasedSwingPropertySource);
        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener).run();
    }

    @Test
    public void testComponentProperty() {
        GuiTestUtils.runOnEDT(() -> {
            String initialValue = "initialValue";
            JButton button = new JButton(initialValue);
            PropertySource<?> property = SwingProperties.componentPropertySource(button, "text", String.class);

            assertEquals(initialValue, property.getValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            button.setText("NEW-VALUE");
            verify(listener).run();
        });
    }

    @Test
    public void testDocumentText() {
        GuiTestUtils.runOnEDT(() -> {
            String initialValue = "initialValue";
            JTextField text = new JTextField(initialValue);
            PropertySource<?> property = SwingProperties.documentText(text.getDocument());

            assertEquals(initialValue, property.getValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            text.setText("NEW-VALUE");
            verify(listener, atLeastOnce()).run();
        });
    }

    @Test
    public void testTextProperty() {
        GuiTestUtils.runOnEDT(() -> {
            String initialValue = "initialValue";
            JTextField text = new JTextField(initialValue);
            PropertySource<?> property = SwingProperties.textProperty(text);
            assertTrue(property instanceof TextComponentProperty);

            assertEquals(initialValue, property.getValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            text.setText("NEW-VALUE");
            verify(listener, atLeastOnce()).run();
        });
    }

    @Test
    public void testButtonSelected() {
        GuiTestUtils.runOnEDT(() -> {
            JCheckBox checkBox = new JCheckBox();
            PropertySource<Boolean> property = SwingProperties.buttonSelected(checkBox);

            assertFalse(property.getValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            checkBox.setSelected(true);
            verify(listener).run();
        });
    }

    @Test
    public void testSliderValue() {
        GuiTestUtils.runOnEDT(() -> {
            JSlider slider = new JSlider();
            slider.setValue(1);
            PropertySource<Integer> property = SwingProperties.sliderValue(slider);

            assertEquals(1, property.getValue().intValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            slider.setValue(2);
            verify(listener).run();
        });
    }

    @Test
    public void testSpinnerValue() {
        GuiTestUtils.runOnEDT(() -> {
            JSpinner spinner = new JSpinner();
            spinner.setValue(1);
            PropertySource<Object> property = SwingProperties.spinnerValue(spinner);

            assertEquals(1, property.getValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            spinner.setValue(2);
            verify(listener).run();
        });
    }

    @Test
    public void testComboBoxSelection() {
        GuiTestUtils.runOnEDT(() -> {
            JComboBox<Integer> comboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
            comboBox.setSelectedItem(1);
            PropertySource<Integer> property = SwingProperties.comboBoxSelection(comboBox);

            assertEquals(1, property.getValue().intValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            verifyZeroInteractions(listener);

            comboBox.setSelectedItem(2);
            verify(listener, atLeastOnce()).run();
        });
    }
}
