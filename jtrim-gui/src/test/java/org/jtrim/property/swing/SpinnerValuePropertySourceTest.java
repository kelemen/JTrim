package org.jtrim.property.swing;

import javax.swing.JSpinner;
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
public class SpinnerValuePropertySourceTest {
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
        JSpinner spinner = new JSpinner();
        spinner.setValue(1);

        PropertySource<Object> property = SpinnerValuePropertySource.createProperty(spinner);

        assertEquals(1, property.getValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        spinner.setValue(2);

        verify(listener, atLeastOnce()).run();

        assertEquals(2, property.getValue());

        listenerRef.unregister();
        spinner.setValue(3);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testStandardProperties() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                testValuePropertyOnEdt();
            }
        });
    }

    @Test
    public void testNullComponent() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    SpinnerValuePropertySource.createProperty(null);
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
                JSpinner spinner = new JSpinner();
                spinner.setValue(1);

                MutableProperty<Object> property = SpinnerValuePropertySource.createProperty(spinner);
                property.setValue(2);
                assertEquals(2, property.getValue());

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);
                verifyZeroInteractions(listener);

                property.setValue(3);
                verify(listener).run();

                assertEquals(3, property.getValue());
            }
        });
    }
}
