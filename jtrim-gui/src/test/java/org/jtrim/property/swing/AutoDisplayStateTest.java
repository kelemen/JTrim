package org.jtrim.property.swing;

import java.awt.Component;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.swing.component.GuiTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AutoDisplayStateTest {
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
    public void testSwingStateListenerCalledFromAppropriateContext() {
        MutableProperty<Boolean> property = PropertyFactory.memProperty(false);
        BoolPropertyListener listener = mock(BoolPropertyListener.class);

        final AtomicBoolean wrongContext = new AtomicBoolean(false);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (!SwingUtilities.isEventDispatchThread()) {
                    wrongContext.set(true);
                }
                return null;
            }
        }).when(listener).onChangeValue(anyBoolean());

        AutoDisplayState.addSwingStateListener(property, listener);

        GuiTestUtils.waitAllSwingEvents();

        verify(listener).onChangeValue(false);
        assertFalse(wrongContext.get());
    }

    private static void verifyLastArgument(BoolPropertyListener listener, int callCount, boolean expectedLastArg) {
        ArgumentCaptor<Boolean> argCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(listener, times(callCount)).onChangeValue(argCaptor.capture());

        assertEquals(expectedLastArg, argCaptor.getValue());
    }

    private void testSwingStateListenerValueChangeOnEdt() {
        assert SwingUtilities.isEventDispatchThread();

        MutableProperty<Boolean> property = PropertyFactory.memProperty(false);
        BoolPropertyListener listener = mock(BoolPropertyListener.class);

        ListenerRef listenerRef = AutoDisplayState.addSwingStateListener(property, listener);
        verifyLastArgument(listener, 1, false);

        property.setValue(true);
        verifyLastArgument(listener, 2, true);

        property.setValue(true);
        verifyLastArgument(listener, 2, true);

        property.setValue(false);
        verifyLastArgument(listener, 3, false);

        listenerRef.unregister();

        property.setValue(true);
        verifyLastArgument(listener, 3, false);
    }

    @Test
    public void testSwingStateListenerValueChange() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                testSwingStateListenerValueChangeOnEdt();
            }
        });
    }

    @Test
    public void testComponentDisabler() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JButton button = new JButton();
                BoolPropertyListener listener = AutoDisplayState.componentDisabler(button);
                assertTrue(listener instanceof ComponentDisabler);

                listener.onChangeValue(false);
                assertFalse(button.isEnabled());

                listener.onChangeValue(true);
                assertTrue(button.isEnabled());
            }
        });
    }

    @Test
    public void testButtonCaptionSetter() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JButton button = new JButton();
                BoolPropertyListener listener = AutoDisplayState.buttonCaptionSetter(
                        button, "BUTTON_TRUE", "BUTTON_FALSE");
                assertTrue(listener instanceof ButtonTextSwitcher);

                listener.onChangeValue(false);
                assertEquals("BUTTON_FALSE", button.getText());

                listener.onChangeValue(true);
                assertEquals("BUTTON_TRUE", button.getText());
            }
        });
    }

    @Test
    public void testGlassPaneSwitcher_RootPaneContainer_GlassPaneFactory() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JPanel glassPane = new JPanel();

                JFrame frame = new JFrame();
                Component originalGlassPane = frame.getGlassPane();
                GlassPaneFactory factory = mock(GlassPaneFactory.class);
                stub(factory.createGlassPane()).toReturn(glassPane);

                BoolPropertyListener listener = AutoDisplayState.glassPaneSwitcher(frame, factory);
                assertTrue(listener instanceof GlassPaneSwitcher);

                listener.onChangeValue(false);
                assertSame(glassPane, frame.getGlassPane());

                listener.onChangeValue(true);
                assertSame(originalGlassPane, frame.getGlassPane());
            }
        });
    }

    @Test
    public void testGlassPaneSwitcher_RootPaneContainer_DelayedGlassPane() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JPanel glassPane = new JPanel();

                JFrame frame = new JFrame();
                Component originalGlassPane = frame.getGlassPane();
                GlassPaneFactory factory = mock(GlassPaneFactory.class);
                stub(factory.createGlassPane()).toReturn(glassPane);
                DelayedGlassPane glassPanes = new DelayedGlassPane(factory, 0, TimeUnit.NANOSECONDS);

                BoolPropertyListener listener = AutoDisplayState.glassPaneSwitcher(frame, glassPanes);
                assertTrue(listener instanceof GlassPaneSwitcher);

                listener.onChangeValue(false);
                assertSame(glassPane, frame.getGlassPane());

                listener.onChangeValue(true);
                assertSame(originalGlassPane, frame.getGlassPane());
            }
        });
    }

    @Test
    public void testGlassPaneSwitcher_JLayer_GlassPaneFactory() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JPanel glassPane = new JPanel();

                JLayer<?> layer = new JLayer<>();
                Component originalGlassPane = layer.getGlassPane();
                GlassPaneFactory factory = mock(GlassPaneFactory.class);
                stub(factory.createGlassPane()).toReturn(glassPane);

                BoolPropertyListener listener = AutoDisplayState.glassPaneSwitcher(layer, factory);
                assertTrue(listener instanceof GlassPaneSwitcher);

                listener.onChangeValue(false);
                assertSame(glassPane, layer.getGlassPane());

                listener.onChangeValue(true);
                assertSame(originalGlassPane, layer.getGlassPane());
            }
        });
    }

    @Test
    public void testGlassPaneSwitcher_JLayer_DelayedGlassPane() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JPanel glassPane = new JPanel();

                JLayer<?> layer = new JLayer<>();
                Component originalGlassPane = layer.getGlassPane();
                GlassPaneFactory factory = mock(GlassPaneFactory.class);
                stub(factory.createGlassPane()).toReturn(glassPane);
                DelayedGlassPane glassPanes = new DelayedGlassPane(factory, 0, TimeUnit.NANOSECONDS);

                BoolPropertyListener listener = AutoDisplayState.glassPaneSwitcher(layer, glassPanes);
                assertTrue(listener instanceof GlassPaneSwitcher);

                listener.onChangeValue(false);
                assertSame(glassPane, layer.getGlassPane());

                listener.onChangeValue(true);
                assertSame(originalGlassPane, layer.getGlassPane());
            }
        });
    }

    /**
     * Test of invisibleGlassPane method, of class AutoDisplayState.
     */
    @Test
    public void testInvisibleGlassPane() {
        assertSame(InvisibleGlassPaneFactory.INSTANCE, AutoDisplayState.invisibleGlassPane());
    }
}
