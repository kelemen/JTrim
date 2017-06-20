package org.jtrim2.property.swing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.testutils.swing.component.GuiTestUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class DocumentTextPropertyTest {
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
        GuiTestUtils.runOnEDT(this::testTextPropertyOnEdt);
    }

    @Test
    public void testEmptyValue() {
        GuiTestUtils.runOnEDT(() -> {
            JTextField textField = new JTextField("");
            PropertySource<?> property = DocumentTextProperty.createProperty(textField.getDocument());

            assertEquals("", property.getValue());
        });
    }

    @Test
    public void testListenerIsNotifiedOnEdt() {
        final AtomicReference<JTextField> textFieldRef = new AtomicReference<>(null);
        GuiTestUtils.runOnEDT(() -> {
            textFieldRef.set(new JTextField());
        });

        PropertySource<String> property = DocumentTextProperty.createProperty(textFieldRef.get().getDocument());

        Runnable listener = mock(Runnable.class);
        final AtomicBoolean onlyFormSwingContext = new AtomicBoolean(true);
        doAnswer((InvocationOnMock invocation) -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                onlyFormSwingContext.set(false);
            }
            return null;
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
        GuiTestUtils.runOnEDT(() -> {
            try {
                DocumentTextProperty.createProperty(null);
                fail("Expected NullPointerException");
            } catch (NullPointerException ex) {
            }
        });
    }

    @Test
    public void testSetText() {
        GuiTestUtils.runOnEDT(() -> {
            JTextField textField = new JTextField("INITIAL");
            MutableProperty<String> property = DocumentTextProperty.createProperty(textField.getDocument());

            Runnable listener = mock(Runnable.class);
            property.addChangeListener(listener);

            String newValue = "NEW-VALUE";
            property.setValue(newValue);
            verify(listener, atLeastOnce()).run();
            assertEquals(newValue, textField.getText());
        });
    }

    @Test
    public void testBadLocationOnSetExceptionIsNotHidden() {
        GuiTestUtils.runOnEDT(() -> {
            Document document = mock(Document.class);
            BadLocationException error = new BadLocationException("", 0);

            try {
                doThrow(error)
                        .when(document)
                        .insertString(anyInt(), any(String.class), any(AttributeSet.class));
                doThrow(error)
                        .when(document)
                        .remove(anyInt(), anyInt());
            } catch (BadLocationException ex) {
                throw new AssertionError(ex.getMessage(), ex);
            }

            MutableProperty<String> property = DocumentTextProperty.createProperty(document);
            try {
                property.setValue("NEW-VALUE");
                fail("Expected failure due to BadLocationException.");
            } catch (Throwable ex) {
                assertSame(error, ex.getCause());
            }
        });
    }

    @Test
    public void testBadLocationOnGetExceptionIsNotHidden() {
        GuiTestUtils.runOnEDT(() -> {
            Document document = mock(Document.class);
            BadLocationException error = new BadLocationException("", 0);

            try {
                doAnswer((InvocationOnMock invocation) -> {
                    Runnable arg = (Runnable) invocation.getArguments()[0];
                    arg.run();
                    return null;
                }).when(document).render(any(Runnable.class));

                doThrow(error)
                        .when(document)
                        .getText(anyInt(), anyInt());
                doThrow(error)
                        .when(document)
                        .getText(anyInt(), anyInt(), any(Segment.class));
            } catch (BadLocationException ex) {
                throw new AssertionError(ex.getMessage(), ex);
            }

            MutableProperty<String> property = DocumentTextProperty.createProperty(document);
            try {
                property.getValue();
                fail("Expected failure due to BadLocationException.");
            } catch (Throwable ex) {
                assertSame(error, ex.getCause());
            }
        });
    }
}
