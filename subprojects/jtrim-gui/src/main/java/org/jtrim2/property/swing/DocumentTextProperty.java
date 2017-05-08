package org.jtrim2.property.swing;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingUpdateTaskExecutor;

/**
 * @see SwingProperties#documentText(Document)
 */
final class DocumentTextProperty implements SwingPropertySource<String, DocumentListener> {
    private final Document document;

    private DocumentTextProperty(Document document) {
        Objects.requireNonNull(document, "document");

        this.document = document;
    }

    public static MutableProperty<String> createProperty(final Document document) {
        PropertySource<String> source = SwingProperties.fromSwingSource(
                new DocumentTextProperty(document),
                new ListenerForwarderFactory());

        return new AbstractMutableProperty<String>(source) {
            @Override
            public void setValue(String value) {
                try {
                    document.remove(0, document.getLength());
                    document.insertString(0, value, null);
                } catch (BadLocationException ex) {
                    throw new ConcurrentModificationException(ex);
                }
            }
        };
    }

    @Override
    public String getValue() {
        final AtomicReference<String> result = new AtomicReference<>();
        document.render(() -> {
            try {
                result.set(document.getText(0, document.getLength()));
            } catch (BadLocationException ex) {
                throw new IllegalStateException("Unexpected state", ex);
            }
        });
        return result.get();
    }

    @Override
    public void addChangeListener(DocumentListener listener) {
        document.addDocumentListener(listener);
    }

    @Override
    public void removeChangeListener(DocumentListener listener) {
        document.removeDocumentListener(listener);
    }

    private static final class ListenerForwarderFactory
    implements
            SwingForwarderFactory<DocumentListener> {

        private final UpdateTaskExecutor listenerExecutor;

        public ListenerForwarderFactory() {
            this.listenerExecutor = new SwingUpdateTaskExecutor(false);
        }

        @Override
        public DocumentListener createForwarder(final Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            return new DocumentListener() {
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
        }
    }
}
