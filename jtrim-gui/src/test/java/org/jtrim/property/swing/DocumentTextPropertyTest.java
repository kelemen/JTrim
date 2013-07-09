package org.jtrim.property.swing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.component.GuiTestUtils;
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
public class DocumentTextPropertyTest {
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

    private void testTextPropertyOnEdt() {
        String initialValue = "initialValue";
        JTextField textField = new JTextField(initialValue);
        PropertySource<?> property = DocumentTextProperty.createProperty(textField.getDocument());

        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        verifyZeroInteractions(listener);

        String newValue = "NEW-VALUE";
        textField.setText(newValue);

        verify(listener, atLeastOnce()).run();

        assertEquals(newValue, property.getValue());

        listenerRef.unregister();
        textField.setText("LAST-VALUE");

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testStandardProperties() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                testTextPropertyOnEdt();
            }
        });
    }

    @Test
    public void testEmptyValue() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JTextField textField = new JTextField("");
                PropertySource<?> property = DocumentTextProperty.createProperty(textField.getDocument());

                assertEquals("", property.getValue());
            }
        });
    }

    @Test
    public void testListenerIsNotifiedOnEdt() {
        final AtomicReference<JTextField> textFieldRef = new AtomicReference<>(null);
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                textFieldRef.set(new JTextField());
            }
        });

        PropertySource<String> property = DocumentTextProperty.createProperty(textFieldRef.get().getDocument());

        Runnable listener = mock(Runnable.class);
        final AtomicBoolean onlyFormSwingContext = new AtomicBoolean(true);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (!SwingUtilities.isEventDispatchThread()) {
                    onlyFormSwingContext.set(false);
                }
                return null;
            }
        }).when(listener).run();

        property.addChangeListener(listener);
        verifyZeroInteractions(listener);

        String newValue = "NEW-VALUE";
        textFieldRef.get().setText(newValue);

        GuiTestUtils.waitAllSwingEvents();

        verify(listener, atLeastOnce()).run();
        assertTrue(onlyFormSwingContext.get());
    }

    @Test
    public void testNullComponent() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    DocumentTextProperty.createProperty(null);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }
}
