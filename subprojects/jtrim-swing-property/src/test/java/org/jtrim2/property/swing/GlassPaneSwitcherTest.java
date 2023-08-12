package org.jtrim2.property.swing;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.property.BoolPropertyListener;
import org.junit.Test;

import static org.junit.Assert.*;

public class GlassPaneSwitcherTest {
    private static DelayedGlassPane constDelayedDecorator(JPanel immediate, JPanel main, long patienceMs) {
        return new DelayedGlassPane(
                new ConstGlassPaneFactory(immediate),
                new ConstGlassPaneFactory(main),
                patienceMs,
                TimeUnit.MILLISECONDS);
    }

    public static void testLayerChangeAccessWithoutDelay(GlassPaneSwitcherFactory factory) throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createLayerTestData(factory);
        SwingUtilities.invokeAndWait(() -> {
            BoolPropertyListener listener = testData.createNoDelay();

            listener.onChangeValue(true);
            testData.checkNoDecorator();

            listener.onChangeValue(false);
            testData.checkMainDecorator();

            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });
    }

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithoutDelay() throws Exception {
        testLayerChangeAccessWithoutDelay(Factory.INSTANCE);
    }

    public static void testFrameChangeAccessWithoutDelay(GlassPaneSwitcherFactory factory) throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createFrameTestData(factory);
        SwingUtilities.invokeAndWait(() -> {
            BoolPropertyListener listener = testData.createNoDelay();

            listener.onChangeValue(true);
            testData.checkNoDecorator();

            listener.onChangeValue(false);
            testData.checkMainDecorator();

            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithoutDelay() throws Exception {
        testFrameChangeAccessWithoutDelay(Factory.INSTANCE);
    }

    public static void testLayerChangeAccessWithDelay(GlassPaneSwitcherFactory factory) throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createLayerTestData(factory);
        final BoolPropertyListener listener = testData.createWithDelay(5);

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(false);
            testData.checkImmediateDecorator();
        });
        testData.waitMainDecorator();

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(false);
            testData.checkImmediateDecorator();

            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });
    }

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithDelay() throws Exception {
        testLayerChangeAccessWithDelay(Factory.INSTANCE);
    }

    public static void testFrameChangeAccessWithDelay(GlassPaneSwitcherFactory factory) throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createFrameTestData(factory);
        final BoolPropertyListener listener = testData.createWithDelay(5);

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(false);
            testData.checkImmediateDecorator();
        });
        testData.waitMainDecorator();

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });

        SwingUtilities.invokeAndWait(() -> {
            listener.onChangeValue(false);
            testData.checkImmediateDecorator();

            listener.onChangeValue(true);
            testData.checkNoDecorator();
        });
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithDelay() throws Exception {
        testFrameChangeAccessWithDelay(Factory.INSTANCE);
    }

    private static TestData createFrameTestData(GlassPaneSwitcherFactory factory) {
        try {
            return createFrameTestDataWithException(factory);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static TestData createLayerTestData(GlassPaneSwitcherFactory factory) {
        try {
            return createLayerTestDataWithException(factory);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private static TestData createFrameTestDataWithException(
            final GlassPaneSwitcherFactory factory) throws Exception {
        final AtomicReference<TestData> resultRef = new AtomicReference<>(null);
        SwingUtilities.invokeAndWait(() -> {
            resultRef.set(new FrameTestData(factory));
        });
        return resultRef.get();
    }

    private static TestData createLayerTestDataWithException(
            final GlassPaneSwitcherFactory factory) throws Exception {

        final AtomicReference<TestData> resultRef = new AtomicReference<>(null);
        SwingUtilities.invokeAndWait(() -> {
            resultRef.set(new LayerTestData(factory));
        });
        return resultRef.get();
    }

    private abstract static class TestData {
        protected final GlassPaneSwitcherFactory factory;

        private final JPanel immediate;
        private final JPanel main;
        private final JPanel noGlassPane;
        private final Lock mainLock;
        private final Condition setGlassPaneCondition;
        private volatile Object currentGlassPane;

        public TestData(GlassPaneSwitcherFactory factory, JPanel noGlassPane) {
            this.factory = factory;
            this.immediate = new JPanel();
            this.main = new JPanel();
            this.noGlassPane = noGlassPane;
            this.currentGlassPane = noGlassPane;
            this.mainLock = new ReentrantLock();
            this.setGlassPaneCondition = mainLock.newCondition();
        }

        protected final void setGlassPane(Object glassPane) {
            mainLock.lock();
            try {
                currentGlassPane = glassPane;
                setGlassPaneCondition.signalAll();
            } finally {
                mainLock.unlock();
            }
        }

        public final BoolPropertyListener createNoDelay() {
            return create1(new ConstGlassPaneFactory(main));
        }

        public final BoolPropertyListener createWithDelay(long patienceMs) {
            return create2(patienceMs, constDelayedDecorator(immediate, main, patienceMs));
        }

        protected abstract BoolPropertyListener create1(GlassPaneFactory mainFactory);
        protected abstract BoolPropertyListener create2(long patienceMs, DelayedGlassPane glassPanes);

        public final void checkImmediateDecorator() {
            assertSame(immediate, currentGlassPane);
        }

        public final void checkMainDecorator() {
            assertSame(main, currentGlassPane);
        }

        public final void checkNoDecorator() {
            assertSame(noGlassPane, currentGlassPane);
        }

        private void waitDecorator(Object glassPane) {
            mainLock.lock();
            try {
                while (glassPane != currentGlassPane) {
                    setGlassPaneCondition.await();
                }
            } catch (InterruptedException ex) {
                throw new OperationCanceledException(ex);
            } finally {
                mainLock.unlock();
            }
        }

        public final void waitMainDecorator() {
            waitDecorator(main);
        }

        public final void waitNoDecorator() {
            waitDecorator(noGlassPane);
        }
    }

    private static class LayerTestData extends TestData {
        private final JLayer<?> layer;

        public LayerTestData(GlassPaneSwitcherFactory factory) {
            this(factory, new JPanel());
        }

        private LayerTestData(GlassPaneSwitcherFactory factory, JPanel noGlassPane) {
            super(factory, noGlassPane);

            this.layer = new JLayer<>(new JPanel());
            this.layer.setGlassPane(noGlassPane);
            this.layer.addPropertyChangeListener("glassPane", (PropertyChangeEvent evt) -> {
                setGlassPane(evt.getNewValue());
            });
        }

        @Override
        protected BoolPropertyListener create1(GlassPaneFactory glassPaneFactory) {
            return factory.create(layer, glassPaneFactory);
        }

        @Override
        protected BoolPropertyListener create2(long patienceMs, DelayedGlassPane glassPanes) {
            return factory.create(layer, glassPanes);
        }
    }

    private static class FrameTestData extends TestData {
        private final JFrame frame;

        public FrameTestData(GlassPaneSwitcherFactory factory) {
            super(factory, null);

            var testData = this;

            JPanel noGlassPane = new JPanel();
            noGlassPane.setVisible(false);

            this.frame = new JFrame() {
                private Component lastSetGlassPane;

                @Override
                public void setGlassPane(Component glassPane) {
                    super.setGlassPane(glassPane != null ? glassPane : noGlassPane);
                    lastSetGlassPane = glassPane;
                    testData.setGlassPane(glassPane);
                }

                @Override
                public Component getGlassPane() {
                    return lastSetGlassPane;
                }
            };
        }

        @Override
        protected BoolPropertyListener create1(GlassPaneFactory mainFactory) {
            return factory.create(frame, mainFactory);
        }

        @Override
        protected BoolPropertyListener create2(long patienceMs, DelayedGlassPane glassPanes) {
            return factory.create(frame, glassPanes);
        }
    }

    private static class ConstGlassPaneFactory implements GlassPaneFactory {
        private final JPanel panel;

        public ConstGlassPaneFactory(JPanel panel) {
            this.panel = panel;
        }

        @Override
        public JPanel createGlassPane() {
            return panel;
        }
    }

    private enum Factory implements GlassPaneSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(RootPaneContainer window, GlassPaneFactory glassPaneFactory) {
            return new GlassPaneSwitcher(window, glassPaneFactory);
        }

        @Override
        public BoolPropertyListener create(RootPaneContainer window, DelayedGlassPane glassPanes) {
            return new GlassPaneSwitcher(window, glassPanes);
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, GlassPaneFactory glassPaneFactory) {
            return new GlassPaneSwitcher(component, glassPaneFactory);
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, DelayedGlassPane glassPanes) {
            return new GlassPaneSwitcher(component, glassPanes);
        }
    }
}
