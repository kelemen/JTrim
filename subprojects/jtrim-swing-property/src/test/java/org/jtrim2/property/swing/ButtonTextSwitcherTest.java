package org.jtrim2.property.swing;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.jtrim2.property.BoolPropertyListener;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ButtonTextSwitcherTest {
    private static JButton mockedButton(String initialCaption, Runnable getTextMock) {
        return new JButton(initialCaption) {
            private static final long serialVersionUID = 1L;
            @Override
            public String getText() {
                getTextMock.run();
                return super.getText();
            }
        };
    }

    public static void testAutoOkCaption(final ButtonTextSwitcherFactory factory) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String initialCaption = "TEST-INITIAL-CAPTION";
            String cancelCaption = "TEST-CANCEL-CAPTION";

            Runnable getTextMock = mock(Runnable.class);
            JButton button = mockedButton(initialCaption, getTextMock);
            BoolPropertyListener switcher = factory.create(button, cancelCaption);
            // The constructor must get the text at construction time.
            verify(getTextMock, atLeast(1)).run();

            switcher.onChangeValue(false);
            assertEquals(cancelCaption, button.getText());

            switcher.onChangeValue(false);
            assertEquals(cancelCaption, button.getText());

            switcher.onChangeValue(true);
            assertEquals(initialCaption, button.getText());
        });
    }

    @Test
    public void testAutoOkCaption() throws Exception {
        testAutoOkCaption(Factory.INSTANCE);
    }

    public static void testUserDefOkCaption(final ButtonTextSwitcherFactory factory) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String initialCaption = "TEST-INITIAL-CAPTION";
            String okCaption = "TEST-OK-CAPTION";
            String cancelCaption = "TEST-CANCEL-CAPTION";

            JButton button = new JButton(initialCaption);
            BoolPropertyListener switcher = factory.create(button, okCaption, cancelCaption);
            assertEquals(initialCaption, button.getText());

            switcher.onChangeValue(true);
            assertEquals(okCaption, button.getText());

            switcher.onChangeValue(false);
            assertEquals(cancelCaption, button.getText());

            switcher.onChangeValue(false);
            assertEquals(cancelCaption, button.getText());

            switcher.onChangeValue(true);
            assertEquals(okCaption, button.getText());
        });
    }

    @Test
    public void testUserDefOkCaption() throws Exception {
        testUserDefOkCaption(Factory.INSTANCE);
    }

    public static void testIllegalConstructorCalls(final ButtonTextSwitcherFactory factory) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                factory.create(null, "CANCEL");
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }

            try {
                factory.create(null, "CANCEL");
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }

            try {
                factory.create(new JButton("INIT"), null);
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }

            try {
                factory.create(null, "OK", "CANCEL");
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }

            try {
                factory.create(new JButton("INIT"), null, "CANCEL");
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }

            try {
                factory.create(new JButton("INIT"), "OK", null);
                fail("Expected: NullPointerException");
            } catch (NullPointerException ex) {
            }
        });
    }

    @Test
    public void testIllegalConstructorCalls() throws Exception {
        testIllegalConstructorCalls(Factory.INSTANCE);
    }

    private enum Factory implements ButtonTextSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(JButton button, String textWhenTrue, String textWhenFalse) {
            return new ButtonTextSwitcher(button, textWhenTrue, textWhenFalse);
        }

        @Override
        public BoolPropertyListener create(JButton button, String textWhenFalse) {
            return new ButtonTextSwitcher(button, button.getText(), textWhenFalse);
        }
    }
}
