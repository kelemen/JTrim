package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncLinks;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataController;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.image.BufferedImages;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.swing.component.GuiTestUtils;
import org.jtrim2.ui.concurrent.query.AsyncRendererFactory;
import org.jtrim2.ui.concurrent.query.GenericAsyncRendererFactory;
import org.jtrim2.ui.concurrent.query.RenderingState;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.testutils.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncRenderingComponentTest {
    private static LogCollector startCollecting() {
        return LogCollector.startCollecting("org.jtrim2");
    }

    private static String getTestState(TestCase test) {
        return "Number of paints: " + test.getNumberOfPaints();
    }

    private static void copyTestImage(BufferedImage destImage) {
        Graphics2D g2d = destImage.createGraphics();
        try {
            BufferedImage image = createTestImage(destImage.getWidth(), destImage.getHeight());
            g2d.drawImage(image, null, 0, 0);
        } finally {
            g2d.dispose();
        }
    }

    private static void clearImage(BufferedImage image) {
        fillImage(image, Color.WHITE);
    }

    private static void checkRenderingStateFinished(final AsyncRenderingComponent component) {
        runOnEDT(() -> {
            assertTrue(component.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
            assertTrue(component.getSignificantRenderingTime(TimeUnit.NANOSECONDS) >= 0);
            assertFalse(component.isRendering());
        });
    }

    @Test
    public void testNotRenderingInitially() {
        GuiTestUtils.runOnEDT(() -> {
            AsyncRenderingComponentImpl component = new AsyncRenderingComponentImpl();
            assertFalse(component.isRendering());
        });
    }

    @Test(timeout = 30000)
    public void testImageRendererStartRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final AtomicBoolean canceled = new AtomicBoolean(true);

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> startRendering(
                        CancellationToken cancelToken,
                        BufferedImage drawingSurface) {
                    canceled.set(cancelToken.isCanceled());
                    copyTestImage(drawingSurface);
                    return RenderingResult.significant(null);
                }

                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(renderer);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });

            verify(renderer).startRendering(
                    any(CancellationToken.class),
                    any(BufferedImage.class));
            verify(renderer, never()).render(
                    any(CancellationToken.class),
                    any(),
                    any(BufferedImage.class));
            assertFalse(canceled.get());

            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testImageRendererFinishRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final AtomicReference<AsyncReport> reportRef = new AtomicReference<>(null);
            final AtomicBoolean canceled = new AtomicBoolean(true);

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    try {
                        canceled.set(cancelToken.isCanceled());
                        reportRef.set(report);
                        copyTestImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(renderer);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });

            verify(renderer).startRendering(
                    any(CancellationToken.class),
                    any(BufferedImage.class));
            verify(renderer, never()).render(
                    any(CancellationToken.class),
                    any(),
                    any(BufferedImage.class));

            assertTrue(reportRef.get().isSuccess());
            assertFalse(canceled.get());

            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testImageRendererRender() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final AtomicBoolean canceled = new AtomicBoolean(true);
            final Object[] datas = new Object[]{new Object(), new Object()};

            AsyncDataLink<Object> dataLink;
            dataLink = (CancellationToken cancelToken, AsyncDataListener<? super Object> dataListener) -> {
                try {
                    for (Object data: datas) {
                        dataListener.onDataArrive(data);
                    }
                } finally {
                    dataListener.onDoneReceive(AsyncReport.SUCCESS);
                }
                return new SimpleDataController();
            };

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> render(
                        CancellationToken cancelToken,
                        Object data,
                        BufferedImage drawingSurface) {
                    canceled.set(cancelToken.isCanceled());

                    copyTestImage(drawingSurface);
                    return RenderingResult.significant(null);
                }

                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.noRendering();
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });

            ArgumentCaptor<Object> dataArgs = ArgumentCaptor.forClass(Object.class);
            InOrder inOrder = inOrder(renderer);
            inOrder.verify(renderer).startRendering(
                    any(CancellationToken.class),
                    any(BufferedImage.class));
            inOrder.verify(renderer, times(2)).render(
                    any(CancellationToken.class),
                    dataArgs.capture(),
                    any(BufferedImage.class));

            assertArrayEquals(datas, dataArgs.getAllValues().toArray());

            assertFalse(canceled.get());
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testRenderAgain() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final AtomicReference<Object> dataRef = new AtomicReference<>(null);

            AsyncDataLink<Object> dataLink;
            dataLink = (CancellationToken cancelToken, AsyncDataListener<? super Object> dataListener) -> {
                try {
                    dataListener.onDataArrive(dataRef.get());
                } finally {
                    dataListener.onDoneReceive(AsyncReport.SUCCESS);
                }
                return new SimpleDataController();
            };

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> render(
                        CancellationToken cancelToken,
                        Object data,
                        BufferedImage drawingSurface) {
                    if (data == null) {
                        clearImage(drawingSurface);
                    } else {
                        try {
                            copyTestImage(drawingSurface);
                        } finally {
                            endSignal.signal();
                        }
                    }
                    // Only the seconds rendering will actually do the
                    // wanted rendering.
                    dataRef.set(new Object());
                    return RenderingResult.significant(null);
                }
            });

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer);
            });
            test.runTest(AsyncRenderingComponent::renderAgain);

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });

            verify(renderer, times(2)).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            verify(renderer, times(2)).render(any(CancellationToken.class), any(), any(BufferedImage.class));
            checkRenderingStateFinished(test.component);
        }
    }

    @SuppressWarnings("unchecked")
    private static AsyncRenderingComponent.PaintHook<Object> mockPaintHook() {
        return mock(AsyncRenderingComponent.PaintHook.class);
    }

    @Test(timeout = 30000)
    public void testWithPaintHookFromStartRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final Object paintResult = new Object();

            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), null);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> startRendering(
                        CancellationToken cancelToken,
                        BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    return RenderingResult.significant(paintResult);
                }
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            when(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .thenReturn(true);

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer, paintHook);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(() -> {
                verify(paintHook, atLeastOnce()).prePaintComponent(
                        any(RenderingState.class), any(Graphics2D.class));
                verify(paintHook, atLeastOnce()).postPaintComponent(
                        any(RenderingState.class),
                        same(paintResult),
                        any(Graphics2D.class));
            });
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testWithPaintHookFromRender() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final Object paintResult = new Object();

            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), null);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> render(
                        CancellationToken cancelToken,
                        Object data,
                        BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    return RenderingResult.significant(paintResult);
                }
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            when(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .thenReturn(true);

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer, paintHook);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(() -> {
                verify(paintHook, atLeastOnce()).prePaintComponent(
                        any(RenderingState.class),
                        any(Graphics2D.class));
                verify(paintHook, atLeastOnce()).postPaintComponent(
                        any(RenderingState.class),
                        same(paintResult),
                        any(Graphics2D.class));
            });
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testWithPaintHookFromFinishRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final Object paintResult = new Object();

            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), null);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            when(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .thenReturn(true);

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer, paintHook);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(() -> {
                verify(paintHook, atLeastOnce()).prePaintComponent(
                        any(RenderingState.class),
                        any(Graphics2D.class));
                verify(paintHook, atLeastOnce()).postPaintComponent(
                        any(RenderingState.class),
                        same(paintResult),
                        any(Graphics2D.class));
            });
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testWithPaintHookSkipRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final Object paintResult = new Object();

            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), null);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();

            test.runTest((final AsyncRenderingComponentImpl component) -> {
                when(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                        .thenAnswer((InvocationOnMock invocation) -> {
                            Object[] args = invocation.getArguments();
                            Graphics2D g2d = (Graphics2D) args[1];

                            BufferedImage image = new BufferedImage(
                                    component.getWidth(),
                                    component.getHeight(),
                                    BufferedImages.getCompatibleBufferType(component.getColorModel()));
                            copyTestImage(image);
                            g2d.drawImage(image, null, 0, 0);
                            return false;
                });
                component.setArgs(dataLink, renderer, paintHook);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });
            verify(paintHook, never())
                    .postPaintComponent(any(RenderingState.class), any(), any(Graphics2D.class));
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testWithPaintHookPaints() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final Object paintResult = new Object();

            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), null);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken,
                        AsyncReport report,
                        BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest((final AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer, new AsyncRenderingComponent.PaintHook<Object>() {
                    @Override
                    public boolean prePaintComponent(RenderingState state, Graphics2D g) {
                        return true;
                    }

                    @Override
                    public void postPaintComponent(
                            RenderingState state, Object renderingResult, Graphics2D g) {
                        BufferedImage image = new BufferedImage(
                                component.getWidth(),
                                component.getHeight(),
                                BufferedImages.getCompatibleBufferType(component.getColorModel()));
                        copyTestImage(image);
                        g.drawImage(image, null, 0, 0);
                    }
                });
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testWithPaintHookAsyncState() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();

            final AsyncDataState state = mock(AsyncDataState.class);
            final AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(new Object(), state);
            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            when(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .thenReturn(true);

            test.runTest((final AsyncRenderingComponentImpl component) -> {
                component.setArgs(dataLink, renderer, paintHook);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(() -> {
                ArgumentCaptor<RenderingState> stateArgs1 = ArgumentCaptor.forClass(RenderingState.class);
                ArgumentCaptor<RenderingState> stateArgs2 = ArgumentCaptor.forClass(RenderingState.class);
                verify(paintHook, atLeastOnce())
                        .prePaintComponent(stateArgs1.capture(), any(Graphics2D.class));
                verify(paintHook, atLeastOnce())
                        .postPaintComponent(stateArgs2.capture(), any(), any(Graphics2D.class));

                assertSame(state, stateArgs1.getValue().getAsyncDataState());
                assertSame(state, stateArgs2.getValue().getAsyncDataState());
            });
        }
    }

    @Test(timeout = 30000)
    public void testFailToInit() {
        ComponentFactory factory = () -> {
            AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl();
            result.setBackground(Color.BLUE);
            return result;
        };
        try (LogCollector logs = startCollecting();
                TestCase test = TestCase.create(factory)) {

            test.runTest(Component::repaint);

            waitAllSwingEvents();

            runOnEDT(() -> {
                BufferedImage content = test.getCurrentContent();
                checkBlankImage(content, Color.BLUE);
            });

            // One for not specifying the async renderer
            // and one for not setting the rendering argument.
            int numberOfLogs = logs.getNumberOfLogs();
            if (numberOfLogs < 2) {
                fail("Expected at least 2 SEVERE logs but received: " + numberOfLogs);
            }
        }
    }

    @Test(timeout = 30000)
    public void testPrePaintListener() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        copyTestImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest((final AsyncRenderingComponentImpl component) -> {
                component.addPrePaintListener(Tasks.runOnceTask(() -> {
                    component.setArgs(renderer);
                }));
                component.repaint();
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });
        }
    }

    @Test(timeout = 30000)
    public void testInitFactoryLater() {
        ComponentFactory factory = () -> {
            AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl(null);
            result.setAsyncRenderer(new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
            result.setArgs(new TestImageRenderer());
            return result;
        };
        try (final TestCase test = TestCase.create(factory)) {
            final WaitableSignal endSignal = new WaitableSignal();

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(
                        CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        copyTestImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest((AsyncRenderingComponentImpl component) -> {
                component.setArgs(renderer);
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(() -> {
                checkTestImagePixels(getTestState(test), test.getCurrentContent());
            });

            verify(renderer).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            verify(renderer, never()).render(any(CancellationToken.class), any(), any(BufferedImage.class));

            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testMultipleInit() {
        runOnEDT(() -> {
            AsyncRenderingComponentImpl component = new AsyncRenderingComponentImpl(null);
            AsyncRendererFactory factory
                    = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());
            component.setAsyncRenderer(factory);
            try {
                component.setAsyncRenderer(factory);
                fail("Expected IllegalStateException.");
            } catch (IllegalStateException ex) {
            }
        });
    }

    @Test(timeout = 30000)
    public void testBuggyInit() {
        runOnEDT(() -> {
            AsyncRenderingComponentImpl component = new AsyncRenderingComponentImpl(null);
            AsyncRendererFactory factory = mock(AsyncRendererFactory.class);
            try {
                component.setAsyncRenderer(factory);
                fail("Expected IllegalArgumentException.");
            } catch (IllegalArgumentException ex) {
            }
        });
    }

    private static class TestImageRenderer implements ImageRenderer<Object, Object> {
        @Override
        public RenderingResult<Object> startRendering(
                CancellationToken cancelToken, BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }

        @Override
        public boolean willDoSignificantRender(Object data) {
            return false;
        }

        @Override
        public RenderingResult<Object> render(
                CancellationToken cancelToken, Object data, BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }

        @Override
        public RenderingResult<Object> finishRendering(
                CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }
    }

    public static final class TestCase implements AutoCloseable {
        private JFrame owner;
        private CapturePaintComponent parent;
        private AsyncRenderingComponentImpl component;

        private TestCase() {
            this.component = null;
        }

        public static TestCase create(final ComponentFactory factory) {
            assert factory != null;

            final TestCase result = new TestCase();
            runOnEDT(() -> {
                result.owner = new JFrame();
                result.owner.setSize(100, 150);
                result.parent = new CapturePaintComponent();
                result.component = factory.create();
                result.owner.setLayout(new GridLayout(1, 1, 0, 0));

                result.parent.setChild(result.component);
                result.owner.add(result.parent);

                result.owner.setVisible(true);
            });
            return result;
        }

        public static TestCase create() {
            return create(() -> {
                AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl(
                        new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                result.setArgs(new TestImageRenderer());
                return result;
            });
        }

        public int getNumberOfPaints() {
            return parent.getNumberOfPaints();
        }

        public BufferedImage getCurrentContent() {
            return parent.getChildContent();
        }

        public void runTest(final TestMethod task) {
            assert task != null;

            runOnEDT(() -> {
                try {
                    task.run(component);
                } catch (Throwable ex) {
                    ExceptionHelper.rethrow(ex);
                }
            });
        }

        @Override
        public void close() {
            runOnEDT(owner::dispose);
        }
    }

    public static interface ComponentFactory {
        public AsyncRenderingComponentImpl create();
    }

    @SuppressWarnings("serial")
    public static class AsyncRenderingComponentImpl extends AsyncRenderingComponent {
        public AsyncRenderingComponentImpl() {
        }

        public AsyncRenderingComponentImpl(AsyncRendererFactory asyncRenderer) {
            super(asyncRenderer);
        }

        public <DataType, ResultType> void setArgs(
                ImageRenderer<? super DataType, ResultType> componentRenderer) {
            setRenderingArgs(componentRenderer);
        }

        public <DataType, ResultType> void setArgs(
                AsyncDataLink<DataType> dataLink,
                ImageRenderer<? super DataType, ResultType> componentRenderer) {
            setRenderingArgs(dataLink, componentRenderer);
        }

        public <DataType, ResultType> void setArgs(
                AsyncDataLink<DataType> dataLink,
                ImageRenderer<? super DataType, ResultType> componentRenderer,
                PaintHook<ResultType> paintHook) {
            setRenderingArgs(dataLink, componentRenderer, paintHook);
        }
    }

    public static interface TestMethod {
        public void run(AsyncRenderingComponentImpl component) throws Throwable;
    }
}
