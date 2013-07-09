package org.jtrim.property.swing;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#documentTextSource(Document)
 *
 * @author Kelemen Attila
 */
final class DocumentTextProperty implements SwingPropertySource<String, DocumentListener> {
    private final Document document;

    private DocumentTextProperty(Document document) {
        ExceptionHelper.checkNotNullArgument(document, "document");

        this.document = document;
    }

    public static PropertySource<String> createProperty(Document document) {
        return SwingProperties.fromSwingSource(new DocumentTextProperty(document), new ListenerForwarderFactory());
    }

    @Override
    public String getValue() {
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException ex) {
            return null;
        }
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