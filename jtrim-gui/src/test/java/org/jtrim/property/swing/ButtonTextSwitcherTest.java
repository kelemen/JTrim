package org.jtrim.property.swing;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.jtrim.property.BoolPropertyListener;
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
public class ButtonTextSwitcherTest {
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

    public static void testAutoOkCaption(final ButtonTextSwitcherFactory factory) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String initialCaption = "TEST-INITIAL-CAPTION";
            String cancelCaption = "TEST-CANCEL-CAPTION";

            JButton button = spy(new JButton(initialCaption));
            BoolPropertyListener switcher = factory.create(button, cancelCaption);
            // The constructor must get the text at construction time.
            verify(button).getText();

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

            JButton button = spy(new JButton(initialCaption));
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
