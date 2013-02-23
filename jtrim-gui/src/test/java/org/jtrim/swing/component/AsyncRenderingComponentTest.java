package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncLinks;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataController;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncRenderingComponentTest {
    private static final int EVENT_LOOP_PATIENCE = 10;

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

    private static void runAfterEvents(final Runnable task) {
        assert task != null;

        final AtomicInteger counter = new AtomicInteger(EVENT_LOOP_PATIENCE);

        Runnable forwardTask = new Runnable() {
            public void executeOrDelay() {
                if (counter.getAndDecrement() <= 0) {
                    task.run();
                }
                else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            executeOrDelay();
                        }
                    });
                }
            }

            @Override
            public void run() {
                executeOrDelay();
            }
        };
        SwingUtilities.invokeLater(forwardTask);
    }

    private static void runOnEDT(final Runnable task) {
        assert task != null;

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            final WaitableSignal doneSignal = new WaitableSignal();
            final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable ex) {
                        errorRef.set(ex);
                    } finally {
                        doneSignal.signal();
                    }
                }
            });
            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            Throwable error = errorRef.get();
            if (error != null) {
                ExceptionHelper.rethrow(error);
            }
        }
    }

    private static void copyTestImage(BufferedImage destImage) {
        Graphics2D g2d = destImage.createGraphics();
        try {
            BufferedImage image = Graphics2DComponentTest.createTestImage(destImage.getWidth(), destImage.getHeight());
            g2d.drawImage(image, null, 0, 0);
        } finally {
            g2d.dispose();
        }
    }

    private static void clearImage(BufferedImage image) {
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    private static void checkRenderingStateFinished(final AsyncRenderingComponent component) {
        runOnEDT(new Runnable() {
            @Override
            public void run() {
                assertTrue(component.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
                assertTrue(component.getSignificantRenderingTime(TimeUnit.NANOSECONDS) >= 0);
                assertFalse(component.isRendering());
            }
        });
    }

    @Test(timeout = 30000)
    public void testImageRendererStartRendering() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();
            final AtomicBoolean canceled = new AtomicBoolean(true);

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> startRendering(CancellationToken cancelToken, BufferedImage drawingSurface) {
                    canceled.set(cancelToken.isCanceled());
                    copyTestImage(drawingSurface);
                    return RenderingResult.significant(null);
                }

                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(renderer);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });

            verify(renderer).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            verify(renderer, never()).render(any(CancellationToken.class), any(), any(BufferedImage.class));
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
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
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

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(renderer);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });

            verify(renderer).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            verify(renderer, never()).render(any(CancellationToken.class), any(), any(BufferedImage.class));

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

            final AsyncDataLink<Object> dataLink = new AsyncDataLink<Object>() {
                @Override
                public AsyncDataController getData(CancellationToken cancelToken, AsyncDataListener<? super Object> dataListener) {
                    try {
                        for (int i = 0; i < datas.length; i++) {
                            dataListener.onDataArrive(datas[i]);
                        }
                    } finally {
                        dataListener.onDoneReceive(AsyncReport.SUCCESS);
                    }
                    return new SimpleDataController();
                }
            };

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> render(CancellationToken cancelToken, Object data, BufferedImage drawingSurface) {
                    canceled.set(cancelToken.isCanceled());

                    copyTestImage(drawingSurface);
                    return RenderingResult.significant(null);
                }
                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.noRendering();
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });

            ArgumentCaptor<Object> dataArgs = ArgumentCaptor.forClass(Object.class);
            InOrder inOrder = inOrder(renderer);
            inOrder.verify(renderer).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            inOrder.verify(renderer, times(2)).render(any(CancellationToken.class), dataArgs.capture(), any(BufferedImage.class));

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

            final AsyncDataLink<Object> dataLink = new AsyncDataLink<Object>() {
                @Override
                public AsyncDataController getData(CancellationToken cancelToken, AsyncDataListener<? super Object> dataListener) {
                    try {
                        dataListener.onDataArrive(dataRef.get());
                    } finally {
                        dataListener.onDoneReceive(AsyncReport.SUCCESS);
                    }
                    return new SimpleDataController();
                }
            };

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> render(CancellationToken cancelToken, Object data, BufferedImage drawingSurface) {
                    if (data == null) {
                        clearImage(drawingSurface);
                    }
                    else {
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

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer);
                }
            });
            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.renderAgain();
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
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
                public RenderingResult<Object> startRendering(CancellationToken cancelToken, BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    return RenderingResult.significant(paintResult);
                }
                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            stub(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .toReturn(true);

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer, paintHook);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    verify(paintHook, atLeastOnce()).prePaintComponent(any(RenderingState.class), any(Graphics2D.class));
                    verify(paintHook, atLeastOnce()).postPaintComponent(any(RenderingState.class),same(paintResult), any(Graphics2D.class));
                }
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
                public RenderingResult<Object> render(CancellationToken cancelToken, Object data, BufferedImage drawingSurface) {
                    clearImage(drawingSurface);
                    return RenderingResult.significant(paintResult);
                }
                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    endSignal.signal();
                    return RenderingResult.noRendering();
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            stub(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .toReturn(true);

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer, paintHook);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    verify(paintHook, atLeastOnce()).prePaintComponent(any(RenderingState.class), any(Graphics2D.class));
                    verify(paintHook, atLeastOnce()).postPaintComponent(any(RenderingState.class),same(paintResult), any(Graphics2D.class));
                }
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
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            stub(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .toReturn(true);

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer, paintHook);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    verify(paintHook, atLeastOnce()).prePaintComponent(any(RenderingState.class), any(Graphics2D.class));
                    verify(paintHook, atLeastOnce()).postPaintComponent(any(RenderingState.class),same(paintResult), any(Graphics2D.class));
                }
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
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();

            test.runTest(new TestMethod() {
                @Override
                public void run(final AsyncRenderingComponentImpl component) {
                    stub(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                            .toAnswer(new Answer<Boolean>() {
                        @Override
                        public Boolean answer(InvocationOnMock invocation) {
                            Object[] args = invocation.getArguments();
                            Graphics2D g2d = (Graphics2D)args[1];

                            BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            copyTestImage(image);
                            g2d.drawImage(image, null, 0, 0);
                            return false;
                        }
                    });
                    component.setArgs(dataLink, renderer, paintHook);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });
            verify(paintHook, never()).postPaintComponent(any(RenderingState.class), any(), any(Graphics2D.class));
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
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(paintResult);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(final AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer, new AsyncRenderingComponent.PaintHook<Object>() {
                        @Override
                        public boolean prePaintComponent(RenderingState state, Graphics2D g) {
                            return true;
                        }

                        @Override
                        public void postPaintComponent(RenderingState state, Object renderingResult, Graphics2D g) {
                            BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            copyTestImage(image);
                            g.drawImage(image, null, 0, 0);
                        }
                    });
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
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
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        clearImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            final AsyncRenderingComponent.PaintHook<Object> paintHook = mockPaintHook();
            stub(paintHook.prePaintComponent(any(RenderingState.class), any(Graphics2D.class)))
                    .toReturn(true);

            test.runTest(new TestMethod() {
                @Override
                public void run(final AsyncRenderingComponentImpl component) {
                    component.setArgs(dataLink, renderer, paintHook);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    ArgumentCaptor<RenderingState> stateArgs1 = ArgumentCaptor.forClass(RenderingState.class);
                    ArgumentCaptor<RenderingState> stateArgs2 = ArgumentCaptor.forClass(RenderingState.class);
                    verify(paintHook, atLeastOnce()).prePaintComponent(stateArgs1.capture(), any(Graphics2D.class));
                    verify(paintHook, atLeastOnce()).postPaintComponent(stateArgs2.capture(), any(), any(Graphics2D.class));

                    assertSame(state, stateArgs1.getValue().getAsyncDataState());
                    assertSame(state, stateArgs2.getValue().getAsyncDataState());
                }
            });
        }
    }

    @Test(timeout = 30000)
    public void testFailToInit() {
        ComponentFactory factory = new ComponentFactory() {
            @Override
            public AsyncRenderingComponentImpl create() {
                AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl();
                result.setBackground(Color.BLUE);
                return result;
            }
        };
        try (final TestCase test = TestCase.create(factory)) {
            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.repaint();
                }
            });

            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    int width = content.getWidth();
                    int height = content.getHeight();

                    int blueRGB = Color.BLUE.getRGB() | 0xFF00_0000;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int rgb = content.getRGB(x, y);
                            if (rgb != blueRGB) {
                                fail("Expected completely blue image but found color: 0x" +  Integer.toHexString(rgb));
                            }
                        }
                    }
                }
            });
            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testPrePaintListener() {
        try (final TestCase test = TestCase.create()) {
            final WaitableSignal endSignal = new WaitableSignal();

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        copyTestImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(final AsyncRenderingComponentImpl component) {
                    component.addPrePaintListener(Tasks.runOnceTask(new Runnable() {
                        @Override
                        public void run() {
                            component.setArgs(renderer);
                        }
                    }, false));
                    component.repaint();
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });
        }
    }

    @Test(timeout = 30000)
    public void testInitFactoryLater() {
        ComponentFactory factory = new ComponentFactory() {
            @Override
            public AsyncRenderingComponentImpl create() {
                AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl(null);
                result.setAsyncRenderer(new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                result.setArgs(new TestImageRenderer());
                return result;
            }
        };
        try (final TestCase test = TestCase.create(factory)) {
            final WaitableSignal endSignal = new WaitableSignal();

            final ImageRenderer<Object, Object> renderer = spy(new TestImageRenderer() {
                @Override
                public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
                    try {
                        copyTestImage(drawingSurface);
                        return RenderingResult.significant(null);
                    } finally {
                        endSignal.signal();
                    }
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncRenderingComponentImpl component) {
                    component.setArgs(renderer);
                }
            });

            endSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage content = test.getCurrentContent();
                    Graphics2DComponentTest.checkTestImagePixels(content);
                }
            });

            verify(renderer).startRendering(any(CancellationToken.class), any(BufferedImage.class));
            verify(renderer, never()).render(any(CancellationToken.class), any(), any(BufferedImage.class));

            checkRenderingStateFinished(test.component);
        }
    }

    @Test(timeout = 30000)
    public void testMultipleInit() {
        runOnEDT(new Runnable() {
            @Override
            public void run() {
                AsyncRenderingComponentImpl component = new AsyncRenderingComponentImpl(null);
                AsyncRendererFactory factory = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());
                component.setAsyncRenderer(factory);
                try {
                    component.setAsyncRenderer(factory);
                    fail("Expected IllegalStateException.");
                } catch (IllegalStateException ex) {
                }
            }
        });
    }

    @Test(timeout = 30000)
    public void testBuggyInit() {
        runOnEDT(new Runnable() {
            @Override
            public void run() {
                AsyncRenderingComponentImpl component = new AsyncRenderingComponentImpl(null);
                AsyncRendererFactory factory = mock(AsyncRendererFactory.class);
                try {
                    component.setAsyncRenderer(factory);
                    fail("Expected IllegalArgumentException.");
                } catch (IllegalArgumentException ex) {
                }
            }
        });
    }

    private static class TestImageRenderer implements ImageRenderer<Object, Object> {
        @Override
        public RenderingResult<Object> startRendering(CancellationToken cancelToken, BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }

        @Override
        public boolean willDoSignificantRender(Object data) {
            return false;
        }

        @Override
        public RenderingResult<Object> render(CancellationToken cancelToken, Object data, BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }

        @Override
        public RenderingResult<Object> finishRendering(CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface) {
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
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    result.owner = new JFrame();
                    result.owner.setSize(100, 150);
                    result.parent = new CapturePaintComponent();
                    result.component = factory.create();
                    result.owner.setLayout(new GridLayout(1, 1, 0, 0));

                    result.parent.setChild(result.component);
                    result.owner.add(result.parent);

                    result.owner.setVisible(true);
                }
            });
            return result;
        }

        public static TestCase create() {
            return create(new ComponentFactory() {
                @Override
                public AsyncRenderingComponentImpl create() {
                    AsyncRenderingComponentImpl result = new AsyncRenderingComponentImpl(
                            new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                    result.setArgs(new TestImageRenderer());
                    return result;
                }
            });
        }

        public BufferedImage getCurrentContent() {
            return parent.getChildContent();
        }

        public void runTest(final TestMethod task) {
            assert task != null;

            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run(component);
                    } catch (Throwable ex) {
                        ExceptionHelper.rethrow(ex);
                    }
                }
            });
        }

        @Override
        public void close() {
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    owner.dispose();
                }
            });
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