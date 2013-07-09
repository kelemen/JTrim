package org.jtrim.property.swing;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#documentText(Document)
 *
 * @author Kelemen Attila
 */
final class DocumentTextProperty implements SwingPropertySource<String, DocumentListener> {
    private final Document document;

    private DocumentTextProperty(Document document) {
        ExceptionHelper.checkNotNullArgument(document, "document");

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
        document.render(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(document.getText(0, document.getLength()));
                } catch (BadLocationException ex) {
                    throw new IllegalStateException("Unexpected state", ex);
                }
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
            ExceptionHelper.checkNotNullArgument(listener, "listener");

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
