package org.jtrim2.property.swing;

import javax.swing.JSpinner;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.testutils.swing.component.GuiTestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpinnerValuePropertySourceTest {
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
        });
    }
}
