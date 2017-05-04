package org.jtrim2.swing.access;

import javax.swing.JButton;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.property.swing.ButtonTextSwitcherFactory;
import org.jtrim2.property.swing.ButtonTextSwitcherTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.swing.access.CompatibilityUtils.*;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("deprecation")
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