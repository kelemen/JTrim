package org.jtrim.swing.access;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.OperationCanceledException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    private static DelayedDecorator constDelayedDecorator(JPanel immediate, JPanel main, long patienceMs) {
        return new DelayedDecorator(
                new ConstDecoratorPanelFactory(immediate),
                new ConstDecoratorPanelFactory(main),
                patienceMs,
                TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithoutDelay() throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createLayerTestData();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ComponentDecorator decorator = testData.createNoDelay();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();

                decorator.onChangeAccess(false);
                testData.checkMainDecorator();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithoutDelay() throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createFrameTestData();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ComponentDecorator decorator = testData.createNoDelay();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();

                decorator.onChangeAccess(false);
                testData.checkMainDecorator();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });
    }

    @Test(timeout = 20000)
    public void testLayerChangeAccessWithDelay() throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createLayerTestData();
        final ComponentDecorator decorator = testData.createWithDelay(5);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(false);
                testData.checkImmediateDecorator();
            }
        });
        testData.waitMainDecorator();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(false);
                testData.checkImmediateDecorator();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });
    }

    @Test(timeout = 20000)
    public void testFrameChangeAccessWithDelay() throws Exception {
        assertFalse("Test method is not expected to be called from the EDT.", SwingUtilities.isEventDispatchThread());

        final TestData testData = createFrameTestData();
        final ComponentDecorator decorator = testData.createWithDelay(5);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(false);
                testData.checkImmediateDecorator();
            }
        });
        testData.waitMainDecorator();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                decorator.onChangeAccess(false);
                testData.checkImmediateDecorator();

                decorator.onChangeAccess(true);
                testData.checkNoDecorator();
            }
        });
    }

    private static TestData createFrameTestData() {
        try {
            return createFrameTestDataWithException();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static TestData createLayerTestData() {
        try {
            return createLayerTestDataWithException();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private static TestData createFrameTestDataWithException() throws Exception {
        final AtomicReference<TestData> resultRef = new AtomicReference<>(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                resultRef.set(new FrameTestData());
            }
        });
        return resultRef.get();
    }

    private static TestData createLayerTestDataWithException() throws Exception {
        final AtomicReference<TestData> resultRef = new AtomicReference<>(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                resultRef.set(new LayerTestData());
            }
        });
        return resultRef.get();
    }

    private static abstract class TestData {
        private final JPanel immediate;
        private final JPanel main;
        private final JPanel noGlassPane;
        private final Lock mainLock;
        private final Condition setGlassPaneCondition;
        private volatile Object currentGlassPane;

        public TestData(JPanel noGlassPane) {
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

        public final ComponentDecorator createNoDelay() {
            return create1(new ConstDecoratorPanelFactory(main));
        }

        public final ComponentDecorator createWithDelay(long patienceMs) {
            return create2(patienceMs, constDelayedDecorator(immediate, main, patienceMs));
        }

        protected abstract ComponentDecorator create1(DecoratorPanelFactory mainFactory);
        protected abstract ComponentDecorator create2(long patienceMs, DelayedDecorator decorator);

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

        public LayerTestData() {
            this(new JPanel());
        }

        private LayerTestData(JPanel noGlassPane) {
            super(noGlassPane);

            this.layer = new JLayer<>(new JPanel());
            this.layer.setGlassPane(noGlassPane);
            this.layer.addPropertyChangeListener("glassPane", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    setGlassPane(evt.getNewValue());
                }
            });
        }

        @Override
        protected ComponentDecorator create1(DecoratorPanelFactory mainFactory) {
            return new ComponentDecorator(layer, mainFactory);
        }

        @Override
        protected ComponentDecorator create2(long patienceMs, DelayedDecorator decorator) {
            return new ComponentDecorator(layer, decorator);
        }
    }

    private static class FrameTestData extends TestData {
        private final JFrame frame;

        public FrameTestData() {
            super(null);

            this.frame = mock(JFrame.class);
            this.frame.setGlassPane(null);

            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    setGlassPane(invocation.getArguments()[0]);
                    return null;
                }
            }).when(frame).setGlassPane(any(JPanel.class));
        }

        @Override
        protected ComponentDecorator create1(DecoratorPanelFactory mainFactory) {
            return new ComponentDecorator(frame, mainFactory);
        }

        @Override
        protected ComponentDecorator create2(long patienceMs, DelayedDecorator decorator) {
            return new ComponentDecorator(frame, decorator);
        }
    }

    private static class ConstDecoratorPanelFactory implements DecoratorPanelFactory {
        private final JPanel panel;

        public ConstDecoratorPanelFactory(JPanel panel) {
            this.panel = panel;
        }

        @Override
        public JPanel createPanel(Component decorated) {
            return panel;
        }
    }
}