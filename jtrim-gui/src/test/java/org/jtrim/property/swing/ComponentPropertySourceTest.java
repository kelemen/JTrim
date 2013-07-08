package org.jtrim.property.swing;

import javax.swing.JButton;
import org.jtrim.event.ListenerRef;
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
public class ComponentPropertySourceTest {
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

    private void testTextProperty(Class<?> valueType) {
        String initialValue = "initialValue";
        JButton button = new JButton(initialValue);
        PropertySource<?> property = ComponentPropertySource.createProperty(button, "text", valueType);

        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        String newValue = "NEW-VALUE";
        button.setText(newValue);

        verify(listener).run();

        assertEquals(newValue, property.getValue());

        listenerRef.unregister();
        button.setText("LAST-VALUE");

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testWithExactClass() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                testTextProperty(String.class);
            }
        });
    }

    @Test
    public void testWithSuperClass() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                testTextProperty(Object.class);
            }
        });
    }

    @Test
    public void testIllegalValueType() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(new JButton(), "text", Integer.class);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ex) {
                }
            }
        });
    }

    @Test
    public void testEmptyPropertyName() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(new JButton(), "", String.class);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ex) {
                }
            }
        });
    }

    @Test
    public void testUnknownPropertyName() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(new JButton(), "thisPropertyDoesNotExists", String.class);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException ex) {
                }
            }
        });
    }

    @Test
    public void testNullComponent() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(null, "text", String.class);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testNullPropertyName() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(new JButton(), null, String.class);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testNullValueType() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    ComponentPropertySource.createProperty(new JButton(), "text", null);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }
}
