package org.jtrim.swing.access;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
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
public class ButtonCancelSwitcherTest {
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

    private static ButtonCancelSwitcher create1(JButton button, String cancelCaption) {
        return new ButtonCancelSwitcher(button, cancelCaption);
    }

    private static ButtonCancelSwitcher create2(JButton button,
            String caption, String cancelCaption) {
        return new ButtonCancelSwitcher(button, caption, cancelCaption);
    }

    @Test
    public void testAutoOkCaption() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String initialCaption = "TEST-INITIAL-CAPTION";
                String cancelCaption = "TEST-CANCEL-CAPTION";

                JButton button = spy(new JButton(initialCaption));
                ButtonCancelSwitcher switcher = create1(button, cancelCaption);
                // The constructor must get the text at construction time.
                verify(button).getText();

                switcher.onChangeAccess(false);
                assertEquals(cancelCaption, button.getText());

                switcher.onChangeAccess(false);
                assertEquals(cancelCaption, button.getText());

                switcher.onChangeAccess(true);
                assertEquals(initialCaption, button.getText());
            }
        });
    }

    @Test
    public void testUserDefOkCaption() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String initialCaption = "TEST-INITIAL-CAPTION";
                String okCaption = "TEST-OK-CAPTION";
                String cancelCaption = "TEST-CANCEL-CAPTION";

                JButton button = spy(new JButton(initialCaption));
                ButtonCancelSwitcher switcher = create2(button, okCaption, cancelCaption);
                assertEquals(initialCaption, button.getText());

                switcher.onChangeAccess(true);
                assertEquals(okCaption, button.getText());

                switcher.onChangeAccess(false);
                assertEquals(cancelCaption, button.getText());

                switcher.onChangeAccess(false);
                assertEquals(cancelCaption, button.getText());

                switcher.onChangeAccess(true);
                assertEquals(okCaption, button.getText());
            }
        });
    }

    @Test
    public void testIllegalConstructor1() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    create1(null, "CANCEL");
                    fail("Expected: NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testIllegalConstructor2() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    create1(new JButton("INIT"), null);
                    fail("Expected: NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testIllegalConstructor3() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    create2(null, "OK", "CANCEL");
                    fail("Expected: NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testIllegalConstructor4() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    create2(new JButton("INIT"), null, "CANCEL");
                    fail("Expected: NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }

    @Test
    public void testIllegalConstructor5() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    create2(new JButton("INIT"), "OK", null);
                    fail("Expected: NullPointerException");
                } catch (NullPointerException ex) {
                }
            }
        });
    }
}