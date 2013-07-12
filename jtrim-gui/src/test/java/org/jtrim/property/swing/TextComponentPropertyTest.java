package org.jtrim.property.swing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
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
public class TextComponentPropertyTest {
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

    private static MutableProperty<String> create(JTextComponent component) {
        return new TextComponentProperty(component);
    }

    private void testTextPropertyOnEdt() {
        String initialValue = "initialValue";
        JTextField textField = new JTextField(initialValue);
        PropertySource<?> property = create(textField);

        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);
        assertTrue(listenerRef.isRegistered());

        verifyZeroInteractions(listener);

        String newValue = "NEW-VALUE";
        textField.setText(newValue);

        verify(listener, atLeastOnce()).run();

        assertEquals(newValue, property.getValue());

        listenerRef.unregister();
        assertFalse(listenerRef.isRegistered());
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
                PropertySource<?> property = create(textField);

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

        PropertySource<String> property = create(textFieldRef.get());

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

        // We must wait because there can be a short time before the text listener
        // is registered with the document, since it is only possible to get
        // the document property on the EDT.
        GuiTestUtils.waitAllSwingEvents();

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
                    create(null);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testSetText() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JTextField textField = new JTextField("INITIAL");
                MutableProperty<String> property = create(textField);

                Runnable listener = mock(Runnable.class);
                property.addChangeListener(listener);

                String newValue = "NEW-VALUE";
                property.setValue(newValue);
                verify(listener, atLeastOnce()).run();
                assertEquals(newValue, textField.getText());
            }
        });
    }

    @Test
    public void testMultipleUnregister() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JTextField textField = new JTextField("INITIAL");
                MutableProperty<String> property = create(textField);

                Runnable listener = mock(Runnable.class);

                ListenerRef listenerRef = property.addChangeListener(listener);
                listenerRef.unregister();
                listenerRef.unregister();
                assertFalse(listenerRef.isRegistered());

                property.setValue("NEW-VALUE");
                verifyZeroInteractions(listener);
            }
        });
    }

    @Test
    public void testDocumentChange() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                String newValue = "NEW-VALUE";
                PlainDocument newDocument = new PlainDocument();
                try {
                    newDocument.insertString(0, newValue, null);
                } catch (BadLocationException ex) {
                    throw new AssertionError(ex.getMessage(), ex);
                }

                JTextField textField = new JTextField("INITIAL");
                MutableProperty<String> property = create(textField);

                final AtomicInteger listenerNotificationCount = new AtomicInteger(0);
                Runnable listener = mock(Runnable.class);
                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        listenerNotificationCount.incrementAndGet();
                        return null;
                    }
                }).when(listener).run();

                ListenerRef listenerRef = property.addChangeListener(listener);

                textField.setDocument(newDocument);
                verify(listener, atLeastOnce()).run();

                assertEquals(newValue, property.getValue());

                int preSetTextNotificationCount = listenerNotificationCount.get();

                String newValue2 = "NEW-VALUE2";
                textField.setText(newValue2);

                assertTrue("Must be notified after changing text after a document change",
                        preSetTextNotificationCount < listenerNotificationCount.get());
                verify(listener, atLeastOnce()).run();

                assertEquals(newValue2, property.getValue());

                listenerRef.unregister();
                textField.setText("LAST-VALUE");
                verifyNoMoreInteractions(listener);
            }
        });
    }

    private static Runnable blockEDT() {
        final WaitableSignal blockSignal = new WaitableSignal();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                blockSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            }
        });

        return new Runnable() {
            @Override
            public void run() {
                blockSignal.signal();
            }
        };
    }

    @Test
    public void testQuickUnregisterNotOnEdt() {
        assertFalse(SwingUtilities.isEventDispatchThread());

        final AtomicReference<JTextField> textFieldRef = new AtomicReference<>(null);
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                textFieldRef.set(new JTextField());
            }
        });

        PropertySource<String> property = create(textFieldRef.get());

        Runnable listener = mock(Runnable.class);

        Runnable edtUnblockTask = blockEDT();
        try {
            ListenerRef listenerRef = property.addChangeListener(listener);
            listenerRef.unregister();
        } finally {
            edtUnblockTask.run();
        }

        GuiTestUtils.waitAllSwingEvents();

        String newValue = "NEW-VALUE";
        textFieldRef.get().setText(newValue);

        verifyZeroInteractions(listener);
    }

    @Test
    public void testNullDocument() {
        GuiTestUtils.runOnEDT(new Runnable() {
            @Override
            public void run() {
                JTextField textField = new NullCapableJTextField();
                MutableProperty<String> property = create(textField);

                final AtomicInteger listenerNotificationCount = new AtomicInteger(0);
                Runnable listener = mock(Runnable.class);
                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        listenerNotificationCount.incrementAndGet();
                        return null;
                    }
                }).when(listener).run();

                ListenerRef listenerRef = property.addChangeListener(listener);

                textField.setDocument(null);
                verify(listener, atLeastOnce()).run();

                int notificationCountBeforeSetDocument = listenerNotificationCount.get();
                textField.setDocument(new PlainDocument());
                verify(listener, atLeastOnce()).run();
                assertTrue(notificationCountBeforeSetDocument < listenerNotificationCount.get());

                int notificationCountBeforeSetText = listenerNotificationCount.get();

                String newValue = "NEW-VALUE";
                textField.setText(newValue);
                verify(listener, atLeastOnce()).run();
                assertTrue(notificationCountBeforeSetText < listenerNotificationCount.get());

                listenerRef.unregister();
                textField.setText("THE-LAST-VALUE");
                verifyNoMoreInteractions(listener);
            }
        });
    }

    @SuppressWarnings("serial")
    private static class NullCapableJTextField extends JTextField {
        private final Document nullDocument = new PlainDocument();

        @Override
        public void setDocument(Document doc) {
            super.setDocument(doc != null ? doc : nullDocument);
        }

        @Override
        public Document getDocument() {
            Document result = super.getDocument();

            if (result == nullDocument) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                // 0: getStackTrace(), 1: getDocument()
                if (stackTrace.length >= 3 && stackTrace[2].getClassName().startsWith("org.jtrim")) {
                    return null;
                }
            }

            return result;
        }
    }
}
