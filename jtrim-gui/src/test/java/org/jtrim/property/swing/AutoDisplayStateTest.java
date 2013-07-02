package org.jtrim.property.swing;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLayer;
import javax.swing.RootPaneContainer;
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

    /**
     * Test of invisibleGlassPane method, of class AutoDisplayState.
     */
    @Test
    public void testInvisibleGlassPane() {
        assertSame(InvisibleGlassPaneFactory.INSTANCE, AutoDisplayState.invisibleGlassPane());
    }

    @Test
    public void testButtonCaptionSetter() throws Exception {
        ButtonTextSwitcherTest.testAutoOkCaption(ButtonTextSwitcherFactoryImpl.INSTANCE);
        ButtonTextSwitcherTest.testUserDefOkCaption(ButtonTextSwitcherFactoryImpl.INSTANCE);
        ButtonTextSwitcherTest.testIllegalConstructorCalls(ButtonTextSwitcherFactoryImpl.INSTANCE);
    }

    @Test(timeout = 20000)
    public void testGlassPaneSwitcher() throws Exception {
        GlassPaneSwitcherTest.testLayerChangeAccessWithoutDelay(GlassPaneSwitcherFactoryImpl.INSTANCE);
        GlassPaneSwitcherTest.testFrameChangeAccessWithoutDelay(GlassPaneSwitcherFactoryImpl.INSTANCE);
        GlassPaneSwitcherTest.testLayerChangeAccessWithDelay(GlassPaneSwitcherFactoryImpl.INSTANCE);
        GlassPaneSwitcherTest.testFrameChangeAccessWithDelay(GlassPaneSwitcherFactoryImpl.INSTANCE);
    }

    @Test
    public void testComponentDisabler() {
        ComponentDisablerTest.testStateChanges1(ComponentDisablerFactoryImpl.INSTANCE);
        ComponentDisablerTest.testStateChanges2(ComponentDisablerFactoryImpl.INSTANCE);
    }

    private enum ButtonTextSwitcherFactoryImpl implements ButtonTextSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(JButton button, String textWhenTrue, String textWhenFalse) {
            return AutoDisplayState.buttonCaptionSetter(button, textWhenTrue, textWhenFalse);
        }

        @Override
        public BoolPropertyListener create(JButton button, String textWhenFalse) {
            return AutoDisplayState.buttonCaptionSetter(button, button.getText(), textWhenFalse);
        }
    }

    private enum ComponentDisablerFactoryImpl implements ComponentDisablerFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(Component[] components) {
            return AutoDisplayState.componentDisabler(components);
        }

        @Override
        public BoolPropertyListener create(List<? extends Component> components) {
            return AutoDisplayState.componentDisabler(components.toArray(new Component[0]));
        }
    }

    private enum GlassPaneSwitcherFactoryImpl implements GlassPaneSwitcherFactory {
        INSTANCE;

        @Override
        public BoolPropertyListener create(RootPaneContainer window, GlassPaneFactory glassPaneFactory) {
            return AutoDisplayState.glassPaneSwitcher(window, glassPaneFactory);
        }

        @Override
        public BoolPropertyListener create(RootPaneContainer window, DelayedGlassPane glassPanes) {
            return AutoDisplayState.glassPaneSwitcher(window, glassPanes);
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, GlassPaneFactory glassPaneFactory) {
            return AutoDisplayState.glassPaneSwitcher(component, glassPaneFactory);
        }

        @Override
        public BoolPropertyListener create(JLayer<?> component, DelayedGlassPane glassPanes) {
            return AutoDisplayState.glassPaneSwitcher(component, glassPanes);
        }
    }
}
