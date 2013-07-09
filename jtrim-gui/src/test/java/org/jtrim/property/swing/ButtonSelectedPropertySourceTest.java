package org.jtrim.property.swing;

import javax.swing.JCheckBox;
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
public class ButtonSelectedPropertySourceTest {
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

    private void testSelectedPropertyOnEdt() {
        JCheckBox checkBox = new JCheckBox();
        PropertySource<Boolean> property = ButtonSelectedPropertySource.createProperty(checkBox);

        assertFalse(property.getValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        checkBox.setSelected(true);

        verify(listener, atLeastOnce()).run();

        assertTrue(checkBox.isSelected());

        listenerRef.unregister();
        checkBox.setSelected(false);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testStandardProperties() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                testSelectedPropertyOnEdt();
            }
        });
    }

    @Test
    public void testNullComponent() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ButtonSelectedPropertySource.createProperty(null);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testSetValue() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JCheckBox checkBox = new JCheckBox();
                MutableProperty<Boolean> property = ButtonSelectedPropertySource.createProperty(checkBox);
                property.setValue(false);
                assertFalse(checkBox.isSelected());

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);
                verifyZeroInteractions(listener);

                property.setValue(true);
                verify(listener).run();

                assertTrue(checkBox.isSelected());
            }
        });
    }
}
