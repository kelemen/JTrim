package org.jtrim.swing.access;

import javax.swing.JLayer;
import javax.swing.RootPaneContainer;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.swing.DelayedGlassPane;
import org.jtrim.property.swing.GlassPaneFactory;
import org.jtrim.property.swing.GlassPaneSwitcherFactory;
import org.jtrim.property.swing.GlassPaneSwitcherTest;
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
public class ComponentDecoratorTest {
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

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithoutDelay() throws Exception {
        GlassPaneSwitcherTest.testLayerChangeAccessWithoutDelay(Factory.INSTANCE);
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithoutDelay() throws Exception {
        GlassPaneSwitcherTest.testFrameChangeAccessWithoutDelay(Factory.INSTANCE);
    }

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithDelay() throws Exception {
        GlassPaneSwitcherTest.testLayerChangeAccessWithDelay(Factory.INSTANCE);
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithDelay() throws Exception {
        GlassPaneSwitcherTest.testFrameChangeAccessWithDelay(Factory.INSTANCE);
    }

    private enum Factory implements GlassPaneSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(RootPaneContainer window, GlassPaneFactory glassPaneFactory) {
            return toBoolPropertyListener(new ComponentDecorator(window, toDecoratorFactory(glassPaneFactory)));
        }

        @Override
        public BoolPropertyListener create(RootPaneContainer window, DelayedGlassPane glassPanes) {
            return toBoolPropertyListener(new ComponentDecorator(window, toDelayedDecorator(glassPanes)));
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, GlassPaneFactory glassPaneFactory) {
            return toBoolPropertyListener(new ComponentDecorator(component, toDecoratorFactory(glassPaneFactory)));
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, DelayedGlassPane glassPanes) {
            return toBoolPropertyListener(new ComponentDecorator(component, toDelayedDecorator(glassPanes)));
        }
    }
}
