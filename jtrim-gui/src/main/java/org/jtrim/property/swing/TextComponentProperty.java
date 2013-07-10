package org.jtrim.property.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class TextComponentProperty implements MutableProperty<String> {
    private static final String DOCUMENT_PROPERTY = "document";

    private final JTextComponent component;

    public TextComponentProperty(JTextComponent component) {
        ExceptionHelper.checkNotNullArgument(component, "component");

        this.component = component;
    }

    @Override
    public void setValue(String value) {
        component.setText(value);
    }

    @Override
    public String getValue() {
        return component.getText();
    }

    private Runnable addDocumentPropertyListener(final Runnable listener) {
        final PropertyChangeListener documentChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                listener.run();
            }
        };

        component.addPropertyChangeListener(DOCUMENT_PROPERTY, documentChangeListener);

        // Returns a task to unregister the listener
        return new Runnable() {
            @Override
            public void run() {
                component.removePropertyChangeListener(DOCUMENT_PROPERTY, documentChangeListener);
            }
        };
    }

    private static Runnable addTextChangeListener(final Document document, final Runnable listener) {
        if (document == null) {
            return Tasks.noOpTask();
        }

        final UpdateTaskExecutor listenerExecutor = new SwingUpdateTaskExecutor(false);
        final DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listenerExecutor.execute(listener);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listenerExecutor.execute(listener);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };

        document.addDocumentListener(documentListener);
        return new Runnable() {
            @Override
            public void run() {
                document.removeDocumentListener(documentListener);
            }
        };
    }

    @Override
    public ListenerRef addChangeListener(final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final AtomicReference<Runnable> currentTextListenerUnregisterTask
                = new AtomicReference<>(Tasks.noOpTask());

        final Runnable updateDocumentTextListener = new Runnable() {
            @Override
            public void run() {
                Document document = component.getDocument();

                Runnable newTextListenerUnregisterTask = addTextChangeListener(document, listener);
                Runnable prevTextListenerUnregisterTask
                        = currentTextListenerUnregisterTask.getAndSet(newTextListenerUnregisterTask);

                if (prevTextListenerUnregisterTask != null) {
                    prevTextListenerUnregisterTask.run();
                }
                else {
                    currentTextListenerUnregisterTask.compareAndSet(newTextListenerUnregisterTask, null);
                    newTextListenerUnregisterTask.run();
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            updateDocumentTextListener.run();
        }
        else {
            // The problem is that we cannot update now because getDocument() is
            // only safe to call from the Event Dispatch Thread.
            // This means, that we may miss some events after this method returns,
            // if this method is called from a thread other than the EDT.
            // Note: getText() also calls getDocument().
            SwingUtilities.invokeLater(updateDocumentTextListener);
        }

        final Runnable documentListenerUnregisterTask = addDocumentPropertyListener(new Runnable() {
            @Override
            public void run() {
                updateDocumentTextListener.run();
                listener.run();
            }
        });

        return new ListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                documentListenerUnregisterTask.run();
                Runnable textListenerUnregisterTask = currentTextListenerUnregisterTask.getAndSet(null);
                if (textListenerUnregisterTask != null) {
                    textListenerUnregisterTask.run();
                }
                registered = false;
            }
        };
    }
}
