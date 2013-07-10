package org.jtrim.property.swing;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import org.jtrim.event.EventListeners;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
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
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                String initialValue = "initialValue";
                JButton button = new JButton(initialValue);
                PropertySource<?> property = SwingProperties.componentPropertySource(button, "text", String.class);

                assertEquals(initialValue, property.getValue());

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);

                verifyZeroInteractions(listener);

                button.setText("NEW-VALUE");
                verify(listener).run();
            }
        });
    }

    @Test
    public void testDocumentText() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                String initialValue = "initialValue";
                JTextField text = new JTextField(initialValue);
                PropertySource<?> property = SwingProperties.documentText(text.getDocument());

                assertEquals(initialValue, property.getValue());

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);

                verifyZeroInteractions(listener);

                text.setText("NEW-VALUE");
                verify(listener, atLeastOnce()).run();
            }
        });
    }

    @Test
    public void testTextProperty() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    @Test
    public void testButtonSelected() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JCheckBox checkBox = new JCheckBox();
                PropertySource<Boolean> property = SwingProperties.buttonSelected(checkBox);

                assertFalse(property.getValue());

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);

                verifyZeroInteractions(listener);

                checkBox.setSelected(true);
                verify(listener).run();
            }
        });
    }
}
