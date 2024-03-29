package org.jtrim2.property.swing;

import javax.swing.JSlider;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.testutils.swing.component.GuiTestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SliderValuePropertySourceTest {
    private void testValuePropertyOnEdt() {
        JSlider slider = new JSlider();
        slider.setValue(1);

        PropertySource<Integer> property = SliderValuePropertySource.createProperty(slider);

        assertEquals(1, property.getValue().intValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyNoInteractions(listener);

        slider.setValue(2);

        verify(listener, atLeastOnce()).run();

        assertEquals(2, property.getValue().intValue());

        listenerRef.unregister();
        slider.setValue(3);

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
                SliderValuePropertySource.createProperty(null);
                fail("Expected NullPointerException");
            } catch (NullPointerException ex) {
            }
        });
    }

    @Test
    public void testSetValue() {
        GuiTestUtils.runOnEDT(() -> {
            JSlider slider = new JSlider();
            slider.setValue(1);

            MutableProperty<Integer> property = SliderValuePropertySource.createProperty(slider);
            property.setValue(2);
            assertEquals(2, property.getValue().intValue());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);
            verifyNoInteractions(listener);

            property.setValue(3);
            verify(listener).run();

            assertEquals(3, property.getValue().intValue());
        });
    }
}
