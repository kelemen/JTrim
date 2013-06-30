package org.jtrim.swing.access;

import javax.swing.JButton;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.swing.ButtonTextSwitcherFactory;
import org.jtrim.property.swing.ButtonTextSwitcherTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim.swing.access.CompatibilityUtils.*;

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
        ButtonTextSwitcherTest.testAutoOkCaption(Factory.INSTANCE);
    }

    @Test
    public void testUserDefOkCaption() throws Exception {
        ButtonTextSwitcherTest.testUserDefOkCaption(Factory.INSTANCE);
    }

    @Test
    public void testIllegalConstructorCalls() throws Exception {
        ButtonTextSwitcherTest.testIllegalConstructorCalls(Factory.INSTANCE);
    }

    private enum Factory implements ButtonTextSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(JButton button, String textWhenTrue, String textWhenFalse) {
            return toBoolPropertyListener(new ButtonCancelSwitcher(button, textWhenTrue, textWhenFalse));
        }

        @Override
        public BoolPropertyListener create(JButton button, String textWhenFalse) {
            return toBoolPropertyListener(new ButtonCancelSwitcher(button, textWhenFalse));
        }
    }
}
