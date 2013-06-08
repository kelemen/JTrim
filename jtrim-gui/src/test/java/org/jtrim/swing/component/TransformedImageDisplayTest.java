package org.jtrim.swing.component;

import org.jtrim.image.transform.TransformationStepInput;
import org.jtrim.image.transform.ImageTransformationStep;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.swing.JFrame;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncQueries;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.CachedAsyncDataQuery;
import org.jtrim.concurrent.async.CachedDataRequest;
import org.jtrim.concurrent.async.CachedLinkRequest;
import org.jtrim.concurrent.async.MultiAsyncDataState;
import org.jtrim.concurrent.async.SimpleDataController;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.ImagePointTransformer;
import org.jtrim.image.transform.TransformationSteps;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.LogCollector;
import org.jtrim.utils.TimeDuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.jtrim.swing.component.GuiTestUtils.checkBlankImage;
import static org.jtrim.swing.component.GuiTestUtils.checkNotBlankImage;
import static org.jtrim.swing.component.GuiTestUtils.checkTestImagePixels;
import static org.jtrim.swing.component.GuiTestUtils.createTestImage;
import static org.jtrim.swing.component.GuiTestUtils.fillImage;
import static org.jtrim.swing.component.GuiTestUtils.runAfterEvents;
import static org.jtrim.swing.component.GuiTestUtils.runOnEDT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TransformedImageDisplayTest {
    private static LogCollector startCollecting() {
        return LogCollector.startCollecting("org.jtrim");
    }

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

    private static String getTestState(TestCase test) {
        return "Number of paints: " + test.getNumberOfPaints();
    }

    @Test
    public void testWithoutTransformation() {
        try (final RawTestCase test = RawTestCase.create()) {
            test.runTest(new TestMethodGeneric<RawTransformedImageDisplay>() {
                @Override
                public void run(RawTransformedImageDisplay component) {
                    component.setBackground(Color.BLUE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NullImage());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testNullImage() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NullImage());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testBackgroundChange() {
        try (final TestCase test = TestCase.create();
                LogCollector logs = startCollecting()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NullImageData());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            component.setBackground(Color.GREEN);
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.GREEN);
                }
            });

            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    @Test
    public void testNullImageData() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NullImageData());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testError() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    ImageReceiveException exception = new ImageReceiveException();
                    component.setBackground(Color.BLACK);
                    component.setForeground(Color.WHITE);
                    component.setFont(new Font("Arial", Font.BOLD, 12));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ErrorImage(exception));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkNotBlankImage(test.getCurrentContent());
                }
            });
        }
    }



    @Test
    public void testNullQueryNullImage() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.imageQuery().setValue(null);
                    component.imageAddress().setValue(null);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testSetInputInSingleCall() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());
                }
            });
        }
    }

    @Test
    public void testSetInputInSeparateCall() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());
                }
            });
        }
    }

    private static TransformationStepInput captureTransformerArg(ImageTransformationStep transf) {
        ArgumentCaptor<TransformationStepInput> input = ArgumentCaptor.forClass(TransformationStepInput.class);

        verify(transf, atLeastOnce()).render(
                any(CancellationToken.class),
                input.capture(),
                any(BufferedImage.class));
        return input.getValue();
    }

    private static void checkPointTransformer(
            ImagePointTransformer pointTransf,
            Point2D testInput,
            Point2D testOutput) throws NoninvertibleTransformException {

        Point2D.Double receivedOutput = new Point2D.Double();
        pointTransf.transformSrcToDest(testInput, receivedOutput);
        assertEquals(testOutput, receivedOutput);

        Point2D.Double receivedInput = new Point2D.Double();
        pointTransf.transformDestToSrc(testOutput, receivedInput);
        assertEquals(testInput, receivedInput);
    }

    private static ImageTransformationStep createBlackTransformation() {
        return createTransformation(blankImage(2, 3, Color.BLACK));
    }

    private static ImageTransformationStep fillWithTestImageTransformation(
            final ImagePointTransformer pointTransformer) {
        return new FillWithTestTransformationStep(pointTransformer);
    }

    private static ImageTransformationStep fillWithTestImageTransformation() {
        return fillWithTestImageTransformation(null);
    }

    @Test
    public void testEditTransformation() {
        try (final TestCase test = TestCase.create()) {
            final ImageTransformationStep transf0
                    = spy(createTransformation(blankImage(5, 4, Color.RED)));
            final ImageTransformationStep transf1
                    = spy(createTransformation(blankImage(6, 7, Color.BLUE)));
            final ImageTransformationStep transf2
                    = spy(fillWithTestImageTransformation());

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(7, 8, Color.GREEN));

                    component.firstStep.setTransformation(createBlackTransformation());
                    component.firstStep.setTransformation(null);

                    component.firstStep.setTransformation(createBlackTransformation());
                    component.firstStep.setTransformation(transf0);

                    TransformationStepDef step3 = component.firstStep.getPosition().addAfter();
                    step3.setTransformation(transf1);

                    TransformationStepDef step2 = step3.getPosition().addBefore();
                    step2.setTransformation(createBlackTransformation());

                    TransformationStepDef step4 = step3.getPosition().addAfter();
                    step4.setTransformation(transf2);

                    step2.removeStep();
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());

                    BufferedImage input0 = captureTransformerArg(transf0).getInputImage().getImage();
                    checkBlankImage(input0, Color.GREEN);

                    BufferedImage input1 = captureTransformerArg(transf1).getInputImage().getImage();
                    checkBlankImage(input1, Color.RED);

                    BufferedImage input2 = captureTransformerArg(transf2).getInputImage().getImage();
                    checkBlankImage(input2, Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testUncachedTransformation() {
        final Point2D.Double inputPoint = new Point2D.Double(4364.0, 2564.0);
        final Point2D.Double point1 = new Point2D.Double(3435.0, 4365.0);
        final Point2D.Double point2 = new Point2D.Double(8395.0, 5738.0);

        ImagePointTransformer pointTransf1 = new TestImagePointTransformer(inputPoint, point1);
        ImagePointTransformer pointTransf2 = new TestImagePointTransformer(point1, point2);

        try (final TestCase test = TestCase.create()) {
            final ImageTransformationStep transf0
                    = spy(createTransformation(blankImage(5, 4, Color.RED), null));
            final ImageTransformationStep transf1
                    = spy(createTransformation(blankImage(6, 7, Color.BLUE), pointTransf1));
            final ImageTransformationStep transf2
                    = spy(fillWithTestImageTransformation(pointTransf2));

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(7, 8, Color.GREEN));

                    component.firstStep.setTransformation(transf0);

                    TransformationStepDef step2 = component.firstStep.getPosition().addAfter();
                    step2.setTransformation(transf1);

                    TransformationStepDef step3 = step2.getPosition().addAfter();
                    step3.setTransformation(transf2);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());

                    BufferedImage input0 = captureTransformerArg(transf0).getInputImage().getImage();
                    checkBlankImage(input0, Color.GREEN);

                    BufferedImage input1 = captureTransformerArg(transf1).getInputImage().getImage();
                    checkBlankImage(input1, Color.RED);

                    BufferedImage input2 = captureTransformerArg(transf2).getInputImage().getImage();
                    checkBlankImage(input2, Color.BLUE);

                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) throws Exception {
                            checkPointTransformer(
                                    component.displayedPointTransformer().getValue(),
                                    inputPoint,
                                    point2);
                        }
                    });
                }
            });
        }
    }

    @Test
    public void testCachedTransformation() {
        try (final TestCase test = TestCase.create()) {
            final AtomicReference<ImageTransformationStep> transf1Ref
                    = new AtomicReference<>(null);
            final AtomicReference<ImageTransformationStep> transf2Ref
                    = new AtomicReference<>(null);

            final AtomicReference<TestInput> inputRef = new AtomicReference<>(null);
            final AtomicReference<BufferedImage> transfInput1Ref = new AtomicReference<>(null);
            final AtomicReference<BufferedImage> transfInput2Ref = new AtomicReference<>(null);

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    BufferedImage transfInput1 = blankImage(component, Color.BLUE);
                    BufferedImage transfInput2 = createTestImage(component.getWidth(), component.getHeight());

                    transfInput1Ref.set(transfInput1);
                    transfInput2Ref.set(transfInput2);

                    ImageTransformationStep transf1 = spy(createTransformation(transfInput1));
                    ImageTransformationStep transf2 = spy(createTransformation(transfInput2));
                    transf1Ref.set(transf1);
                    transf2Ref.set(transf2);

                    final CachedAsyncDataQuery<CachedDataRequest<TestInput>, ImageResult> cachedQuery
                            = AsyncQueries.cacheLinks(AsyncQueries.cacheResults(createTestQuery()));

                    AsyncDataQuery<TestInput, ImageResult> min60CachedQuery
                            = new AsyncDataQuery<TestInput, ImageResult>() {
                        @Override
                        public AsyncDataLink<ImageResult> createDataLink(TestInput arg) {
                            CachedDataRequest<TestInput> dataRequest
                                    = new CachedDataRequest<>(arg, ReferenceType.HardRefType);

                            CachedLinkRequest<CachedDataRequest<TestInput>> linkRequest
                                    = new CachedLinkRequest<>(dataRequest);

                            return cachedQuery.createDataLink(linkRequest);
                        }
                    };

                    TestInput input = new ClearImage(component, Color.GREEN);
                    inputRef.set(input);

                    component.imageQuery().setValue(min60CachedQuery);
                    component.imageAddress().setValue(input);

                    TransformationStepInput.Cmp cacheCmp = new TransformationStepInput.Cmp() {
                        @Override
                        public boolean isSameInput(TransformationStepInput input1, TransformationStepInput input2) {
                            return input1.getInputImage().getImage() == input2.getInputImage().getImage();
                        }
                    };
                    component.firstStep.setTransformation(TransformationSteps.cachedStep(
                            ReferenceType.HardRefType, transf1, cacheCmp));

                    TransformationStepDef step2 = component.firstStep.getPosition().addAfter();
                    step2.setTransformation(TransformationSteps.cachedStep(
                            ReferenceType.HardRefType, transf2, cacheCmp));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            component.renderAgain();
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());

                    BufferedImage input1 = captureTransformerArg(transf1Ref.get()).getInputImage().getImage();
                    checkBlankImage(input1, Color.GREEN);

                    BufferedImage input2 = captureTransformerArg(transf2Ref.get()).getInputImage().getImage();
                    checkBlankImage(input2, Color.BLUE);

                    // Verify that no transformation has been applied multiple times.
                    assertEquals(1, inputRef.get().getDataRequestCount());
                    verify(transf1Ref.get()).render(
                            any(CancellationToken.class),
                            any(TransformationStepInput.class),
                            any(BufferedImage.class));
                    verify(transf2Ref.get()).render(
                            any(CancellationToken.class),
                            any(TransformationStepInput.class),
                            any(BufferedImage.class));
                }
            });
        }
    }

    @Test
    public void testLongRenderingLate() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.longRenderingTimeout().setValue(new TimeDuration(0, TimeUnit.NANOSECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());
                }
            });
        }
    }

    @Test
    public void testLongRenderingDisplaysSomething() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.longRenderingTimeout().setValue(null);
                    component.longRenderingTimeout().setValue(new TimeDuration(Long.MAX_VALUE, TimeUnit.DAYS));
                    component.longRenderingTimeout().setValue(new TimeDuration(0, TimeUnit.NANOSECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NeverTerminatingInput());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkNotBlankImage(test.getCurrentContent());
                }
            });
        }
    }

    @Test
    public void testInfiniteRendering() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.longRenderingTimeout().setValue(new TimeDuration(0, TimeUnit.NANOSECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NeverTerminatingInput());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            component.longRenderingTimeout().setValue(null);
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testHideOldImage() {
        ComponentFactory<TransformedImageDisplayImpl> factory = new ComponentFactory<TransformedImageDisplayImpl>() {
            @Override
            public TransformedImageDisplayImpl create() {
                AsyncRendererFactory renderer
                        = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

                TransformedImageDisplayImpl result = new TransformedImageDisplayImpl(renderer) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void displayLongRenderingState(
                            Graphics2D g,
                            MultiAsyncDataState dataStates) {
                    }
                };
                return result;
            }
        };

        try (final TestCase test = TestCase.create(factory)) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.longRenderingTimeout().setValue(new TimeDuration(0, TimeUnit.NANOSECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.oldImageHideTime().setValue(new TimeDuration(0, TimeUnit.NANOSECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NeverTerminatingInput());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.BLUE);
                }
            });
        }
    }

    private void testImageAfterLongRendering(final AsyncDataState initialState) {
        final TaskExecutorService executor1 = new ThreadPoolTaskExecutor("TEST-POOL1-testLongRendering", 1);
        final TaskExecutorService executor2 = new ThreadPoolTaskExecutor("TEST-POOL2-testLongRendering", 1);
        ComponentFactory<TransformedImageDisplayImpl> factory = new ComponentFactory<TransformedImageDisplayImpl>() {
            @Override
            public TransformedImageDisplayImpl create() {
                TransformedImageDisplayImpl display = new TransformedImageDisplayImpl();
                display.setAsyncRenderer(new GenericAsyncRendererFactory(executor1));
                return display;
            }
        };
        try (final TestCase test = TestCase.create(factory)) {
            final AtomicReference<AsyncTestImage> testImageRef = new AtomicReference<>(null);
            final WaitableSignal imageShowSignal = new WaitableSignal();

            test.runTest(new TestMethod() {
                @Override
                public void run(final TransformedImageDisplayImpl component) {
                    AsyncTestImage testImage = new AsyncTestImage(executor2, 1000, component, initialState);
                    testImageRef.set(testImage);

                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.longRenderingTimeout().setValue(new TimeDuration(200, TimeUnit.MILLISECONDS));
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(testImage);
                    component.imageShown().addChangeListener(new Runnable() {
                        @Override
                        public void run() {
                            if (component.imageShown().getValue()) {
                                imageShowSignal.signal();
                            }
                        }
                    });
                }
            });

            imageShowSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(getTestState(test), test.getCurrentContent());
                }
            });
        } finally {
            executor1.shutdown();
            executor2.shutdown();
            executor1.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
            executor2.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test(timeout = 30000)
    public void testImageAfterLongRendering() {
        try (LogCollector logs = startCollecting()) {
            testImageAfterLongRendering(null);
            testImageAfterLongRendering(new SimpleDataState("STARTED", 0.0));
            testImageAfterLongRendering(new MultiAsyncDataState(
                    new SimpleDataState("STATE1", 0.0),
                    new SimpleDataState("STATE2", 0.0),
                    new SimpleDataState("STATE3", 0.0)));

            assertEquals(0, logs.getNumberOfLogs(Level.WARNING));
            assertEquals(0, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    @Test(timeout = 30000)
    public void testTransformationsReuseBuffers() {
        final BufferedImage stepsResult = new BufferedImage(3, 4, BufferedImage.TYPE_INT_ARGB);
        final Set<BufferedImage> offeredBuffers = Collections.newSetFromMap(
                new IdentityHashMap<BufferedImage, Boolean>());
        final AtomicInteger nullBufferCount = new AtomicInteger(0);

        final ImageTransformationStep step = new ImageTransformationStep() {
            @Override
            public TransformedImage render(
                    CancellationToken cancelToken,
                    TransformationStepInput input,
                    BufferedImage offeredBuffer) {
                if (offeredBuffer != null) {
                    offeredBuffers.add(offeredBuffer);
                }
                else {
                    nullBufferCount.incrementAndGet();
                }

                BufferedImage result = offeredBuffer != null ? offeredBuffer : stepsResult;
                return new TransformedImage(result, null);
            }
        };

        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.tmpBufferReferenceType().setValue(ReferenceType.HardRefType);
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(3, 4));

                    component.firstStep.setTransformation(step);

                    TransformationStepDef step2 = component.firstStep.getPosition().addAfter();
                    step2.setTransformation(step);

                    TransformationStepDef step3 = step2.getPosition().addAfter();
                    step3.setTransformation(step);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            component.renderAgain();
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    assertEquals("Transformations must reuse buffers", 1, offeredBuffers.size());
                    assertTrue("Must not pass null more than once.", nullBufferCount.get() <= 1);
                    assertTrue(offeredBuffers.contains(stepsResult));
                }
            });
        }
    }

    private static ImageTransformationStep bufferCheckerTransformation(
            final String name,
            final int width,
            final int height,
            final int bufferType,
            final Collection<Throwable> errors) {

        return new ImageTransformationStep() {
            private final BufferedImage result
                    = new BufferedImage(width, height, bufferType);
            private boolean hasReturned = false;

            @Override
            public TransformedImage render(
                    CancellationToken cancelToken,
                    TransformationStepInput input,
                    BufferedImage offeredBuffer) {
                if (offeredBuffer != null && offeredBuffer != result && hasReturned) {
                    errors.add(new RuntimeException(name + " received an unexpected buffer."));
                }

                hasReturned = true;
                return new TransformedImage(result, null);
            }
        };
    }

    @Test(timeout = 30000)
    public void testTransformationsReuseDifferentBuffers() {
        final Collection<Throwable> errors = new ConcurrentLinkedQueue<>();

        final ImageTransformationStep transf1 = bufferCheckerTransformation(
                "Transformation1", 3, 4, BufferedImage.TYPE_INT_ARGB, errors);
        final ImageTransformationStep transf2 = bufferCheckerTransformation(
                "Transformation2", 3, 5, BufferedImage.TYPE_INT_ARGB, errors);
        final ImageTransformationStep transf3 = bufferCheckerTransformation(
                "Transformation3", 2, 5, BufferedImage.TYPE_INT_ARGB, errors);
        final ImageTransformationStep transf4 = bufferCheckerTransformation(
                "Transformation4", 2, 5, BufferedImage.TYPE_INT_RGB, errors);

        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.BLUE);
                    component.setForeground(Color.WHITE);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(3, 4));

                    component.firstStep.setTransformation(transf1);

                    TransformationStepDef step2 = component.firstStep.getPosition().addAfter();
                    step2.setTransformation(transf2);

                    TransformationStepDef step3 = step2.getPosition().addAfter();
                    step3.setTransformation(transf3);

                    TransformationStepDef step4 = step3.getPosition().addAfter();
                    step4.setTransformation(transf4);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            component.renderAgain();
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    if (!errors.isEmpty()) {
                        RuntimeException ex = new RuntimeException("Buffer errors");
                        for (Throwable error: errors) {
                            ex.addSuppressed(error);
                        }
                        throw ex;
                    }
                }
            });
        }
    }

    @Test
    public void testAddFirstStepCalledTwice() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    try {
                        component.addFirstStep();
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                    }
                }
            });
        }
    }

    @Test
    public void testIllegalProperties() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    try {
                        component.imageQuery().setValue(null);
                        component.imageAddress().setValue(new ClearImage(component));
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                    }

                    try {
                        component.imageAddress().setValue(new ClearImage(component));
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                    }

                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(component));
                    try {
                        component.imageQuery().setValue(null);
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                    }
                }
            });
        }
    }

    @Test
    public void testSimpleProperties() {
        final ClearImage input = new ClearImage(5, 6);

        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.oldImageHideTime().setValue(new TimeDuration(2, TimeUnit.DAYS));

                    for (TimeUnit unit: TimeUnit.values()) {
                        long value = component.oldImageHideTime().getValue().getDuration(unit);
                        assertEquals(value, unit.convert(2, TimeUnit.DAYS));
                    }

                    assertNull(component.imageMetaData().getValue());

                    TestQuery query = createTestQuery();

                    component.imageQuery().setValue(query);
                    component.imageAddress().setValue(input);
                    assertSame(input, component.imageAddress().getValue());
                    assertSame(query, component.imageQuery().getValue());
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) throws InterruptedException {
                            assertSame(input.getMetaData(), component.imageMetaData().getValue());

                            Thread.sleep(5);
                            assertTrue(component.getTimeSinceImageChange(TimeUnit.NANOSECONDS) > 0);
                            assertTrue(component.getTimeSinceLastImageShow(TimeUnit.NANOSECONDS) > 0);
                        }
                    });
                }
            });
        }
    }

    @Test
    public void testTransformationEvent() {
        try (final TestCase test = TestCase.create()) {
            final Runnable transformationListener = mock(Runnable.class);

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new NullImage());

                    component.addTransformationChangeListener(transformationListener);

                    component.imageQuery().setValue(createTestQuery());
                    verify(transformationListener).run();

                    component.imageAddress().setValue(new NullImage());
                    verify(transformationListener, times(2)).run();

                    component.firstStep.setTransformation(createBlackTransformation());
                    verify(transformationListener, times(3)).run();
                }
            });
        }
    }

    @Test
    public void testListeners() {
        final ClearImage input = new ClearImage(5, 6, Color.RED);

        try (final TestCase test = TestCase.create()) {
            final Runnable dummyMetaDataListener = mock(Runnable.class);
            final Runnable dummyImageAddressListener = mock(Runnable.class);
            final Runnable dummyImageLinkListener = mock(Runnable.class);

            final Runnable imageMetaDataListener = mock(Runnable.class);
            final Runnable imageAddressListener = mock(Runnable.class);
            final Runnable imageLinkListener = mock(Runnable.class);

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.imageAddress().addChangeListener(imageAddressListener);
                    component.imageSource().addChangeListener(imageLinkListener);
                    component.imageMetaData().addChangeListener(imageMetaDataListener);

                    component.imageAddress().addChangeListener(dummyImageAddressListener)
                            .unregister();
                    component.imageSource().addChangeListener(dummyImageLinkListener)
                            .unregister();
                    component.imageMetaData().addChangeListener(dummyMetaDataListener)
                            .unregister();

                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(component, Color.BLUE));
                    verify(imageAddressListener).run();
                    verify(imageLinkListener).run();

                    component.imageAddress().setValue(input);
                    verify(imageAddressListener, times(2)).run();
                    verify(imageLinkListener, times(2)).run();
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(TransformedImageDisplayImpl component) {
                            verify(imageMetaDataListener, atLeastOnce()).run();
                        }
                    });
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(TransformedImageDisplayImpl component) {
                    component.setBackground(Color.GREEN);
                    component.imageAddress().setValue(null);
                    verify(imageAddressListener, times(3)).run();
                    verify(imageLinkListener, times(3)).run();
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkBlankImage(test.getCurrentContent(), Color.GREEN);
                }
            });

            verifyZeroInteractions(
                    dummyImageAddressListener,
                    dummyImageLinkListener,
                    dummyMetaDataListener);
        }
    }

    private static ImageTransformationStep createTransformation(BufferedImage input) {
        return new TestTransformationStep(input);
    }

    private static ImageTransformationStep createTransformation(
            BufferedImage input,
            ImagePointTransformer pointTransformer) {
        return new TestTransformationStep(input, pointTransformer);
    }

    public static TestQuery createTestQuery() {
        return TestQuery.INSTANCE;
    }

    private static class TestTransformationStep
    implements
            ImageTransformationStep {
        private final BufferedImage transfromationResult;
        private final ImagePointTransformer pointTransformer;

        public TestTransformationStep(BufferedImage transfromationResult) {
            this(transfromationResult, null);
        }

        public TestTransformationStep(
                BufferedImage transfromationResult,
                ImagePointTransformer pointTransformer) {
            assert transfromationResult != null;

            this.transfromationResult = transfromationResult;
            this.pointTransformer = pointTransformer;
        }

        @Override
        public TransformedImage render(
                CancellationToken cancelToken,
                TransformationStepInput input,
                BufferedImage offeredBuffer) {
            BufferedImage result = ImageData.cloneImage(transfromationResult);
            return new TransformedImage(result, pointTransformer);
        }
    }

    public enum TestQuery implements AsyncDataQuery<TestInput, ImageResult> {
        INSTANCE;

        @Override
        public AsyncDataLink<ImageResult> createDataLink(TestInput arg) {
            return arg.createLink();
        }
    }

    private static final class RawTestCase extends TestCaseGeneric<RawTransformedImageDisplay>  {
        public static RawTestCase create() {
            return create(new ComponentFactory<RawTransformedImageDisplay>() {
                @Override
                public RawTransformedImageDisplay create() {
                    RawTransformedImageDisplay result = new RawTransformedImageDisplay(
                            new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                    return result;
                }
            });
        }

        public static RawTestCase create(ComponentFactory<RawTransformedImageDisplay> factory) {
            RawTestCase result = new RawTestCase();
            init(result, factory);
            return result;
        }
    }

    private static final class TestCase extends TestCaseGeneric<TransformedImageDisplayImpl>  {
        public static TestCase create() {
            return create(new ComponentFactory<TransformedImageDisplayImpl>() {
                @Override
                public TransformedImageDisplayImpl create() {
                    TransformedImageDisplayImpl result = new TransformedImageDisplayImpl(
                            new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                    return result;
                }
            });
        }

        public static TestCase create(ComponentFactory<TransformedImageDisplayImpl> factory) {
            TestCase result = new TestCase();
            init(result, factory);
            return result;
        }
    }

    public static class TestCaseGeneric<ComponentType extends TransformedImageDisplay<TestInput>>
    implements
            AutoCloseable {
        private JFrame owner;
        private CapturePaintComponent parent;
        private ComponentType component;

        protected TestCaseGeneric() {
            this.component = null;
        }

        public static <ComponentType extends TransformedImageDisplay<TestInput>> void init(
                final TestCaseGeneric<ComponentType> result,
                final ComponentFactory<ComponentType> factory) {

            assert result != null;
            assert factory != null;

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
        }

        public final int getNumberOfPaints() {
            return parent.getNumberOfPaints();
        }

        public final BufferedImage getCurrentContent() {
            return parent.getChildContent();
        }

        public final void runTest(final TestMethodGeneric<ComponentType> task) {
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
        public final void close() {
            runOnEDT(new Runnable() {
                @Override
                public void run() {
                    owner.dispose();
                }
            });
        }
    }

    public static interface ComponentFactory<ComponentType> {
        public ComponentType create();
    }

    public static interface TestMethodGeneric<ComponentType extends TransformedImageDisplay<TestInput>>  {
        public void run(ComponentType component) throws Throwable;
    }

    private static interface TestMethod extends TestMethodGeneric<TransformedImageDisplayImpl> {
        @Override
        public void run(TransformedImageDisplayImpl component) throws Throwable;
    }

    public static interface TestInput {
        public AsyncDataLink<ImageResult> createLink();
        public int getDataRequestCount();
    }

    public abstract static class AbstractTestInput implements TestInput {
        private final AtomicInteger requestCount = new AtomicInteger(0);

        @Override
        public final int getDataRequestCount() {
            return requestCount.get();
        }

        protected final AsyncDataLink<ImageResult> createPreparedLink(
                final ImageResult data,
                final AsyncDataState state) {
            return new AsyncDataLink<ImageResult>() {
                @Override
                public AsyncDataController getData(
                        CancellationToken cancelToken,
                        AsyncDataListener<? super ImageResult> dataListener) {

                    try {
                        requestCount.incrementAndGet();
                        dataListener.onDataArrive(data);
                    } finally {
                        dataListener.onDoneReceive(AsyncReport.SUCCESS);
                    }
                    return new SimpleDataController(state);
                }
            };
        }
    }

    public static final class ErrorImage extends AbstractTestInput {
        private final Exception exception;

        public ErrorImage(Exception exception) {
            this.exception = exception;
        }

        @Override
        public AsyncDataLink<ImageResult> createLink() {
            return new AsyncDataLink<ImageResult>() {
                @Override
                public AsyncDataController getData(
                        CancellationToken cancelToken,
                        AsyncDataListener<? super ImageResult> dataListener) {

                    dataListener.onDoneReceive(AsyncReport.getReport(exception, false));
                    return new SimpleDataController();
                }
            };
        }
    }

    public static final class NullImage extends AbstractTestInput {
        @Override
        public AsyncDataLink<ImageResult> createLink() {
            return createPreparedLink(new ImageResult(null, null), null);
        }
    }

    public static final class NullImageData extends AbstractTestInput {
        @Override
        public AsyncDataLink<ImageResult> createLink() {
            return createPreparedLink(null, null);
        }
    }

    public static BufferedImage blankImage(Component component, Color color) {
        return blankImage(component.getWidth(), component.getHeight(), color);
    }

    public static BufferedImage blankImage(int width, int height, Color color) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fillImage(result, color);
        return result;
    }

    public static final class ClearImage extends AbstractTestInput {
        private final int width;
        private final int height;
        private final Color color;
        private final ImageMetaData metaData;

        public ClearImage(Component component) {
            this(component, Color.BLUE);
        }

        public ClearImage(int width, int height) {
            this(width, height, Color.BLUE);
        }

        public ClearImage(Component component, Color color) {
            this(component.getWidth(), component.getHeight(), color);
        }

        public ClearImage(int width, int height, Color color) {
            assert color != null;

            this.width = width;
            this.height = height;
            this.color = color;
            this.metaData = new ImageMetaData(width, height, true);
        }

        public ImageMetaData getMetaData() {
            return metaData;
        }

        @Override
        public AsyncDataLink<ImageResult> createLink() {
            BufferedImage testImg = blankImage(width, height, color);
            return createPreparedLink(new ImageResult(testImg, metaData), null);
        }
    }

    public static final class TestImage extends AbstractTestInput {
        private final int width;
        private final int height;

        public TestImage(Component component) {
            this(component.getWidth(), component.getHeight());
        }

        public TestImage(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public AsyncDataLink<ImageResult> createLink() {
            BufferedImage testImg = createTestImage(width, height);
            ImageMetaData metaData = new ImageMetaData(width, height, true);
            return createPreparedLink(new ImageResult(testImg, metaData), null);
        }
    }

    public static final class AsyncTestImage extends AbstractTestInput {
        private final TaskExecutor executor;
        private final long waitBeforeDataMs;
        private final int width;
        private final int height;
        private final WaitableSignal firstDoneSignal = new WaitableSignal();
        private final AsyncDataState initialState;

        public AsyncTestImage(TaskExecutor executor, int waitBeforeDataMs, Component component) {
            this(executor, waitBeforeDataMs, component.getWidth(), component.getHeight());
        }

        public AsyncTestImage(TaskExecutor executor, int waitBeforeDataMs, int width, int height) {
            this(executor, waitBeforeDataMs, width, height, new SimpleDataState("STARTED", 0.0));
        }

        public AsyncTestImage(
                TaskExecutor executor,
                int waitBeforeDataMs,
                Component component,
                AsyncDataState state) {
            this(executor, waitBeforeDataMs, component.getWidth(), component.getHeight(), state);
        }

        public AsyncTestImage(
                TaskExecutor executor,
                int waitBeforeDataMs,
                int width,
                int height,
                AsyncDataState initialState) {
            this.executor = executor;
            this.waitBeforeDataMs = waitBeforeDataMs;
            this.width = width;
            this.height = height;
            this.initialState = initialState;
        }

        public void waitForFirstTransferComplete() {
            firstDoneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        }

        @Override
        public AsyncDataLink<ImageResult> createLink() {
            BufferedImage testImg = createTestImage(width, height);
            final ImageMetaData metaData = new ImageMetaData(width, height, true);
            final ImageResult data = new ImageResult(testImg, metaData);

            return new AsyncDataLink<ImageResult>() {
                @Override
                public AsyncDataController getData(
                        CancellationToken cancelToken,
                        final AsyncDataListener<? super ImageResult> dataListener) {

                    final SimpleDataController controller = new SimpleDataController();
                    controller.setDataState(initialState);

                    executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                        @Override
                        public void execute(CancellationToken cancelToken) {
                            dataListener.onDataArrive(new ImageResult(null, metaData));
                            CancelableWaits.sleep(cancelToken, waitBeforeDataMs, TimeUnit.MILLISECONDS);
                            dataListener.onDataArrive(data);
                            controller.setDataState(new SimpleDataState("DONE", 1.0));
                        }
                    }, new CleanupTask() {
                        @Override
                        public void cleanup(boolean canceled, Throwable error) {
                            try {
                                dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
                            } finally {
                                firstDoneSignal.signal();
                            }
                        }
                    });
                    return controller;
                }
            };
        }
    }

    private static class NeverTerminatingInput extends AbstractTestInput {
        @Override
        public AsyncDataLink<ImageResult> createLink() {
            return new AsyncDataLink<ImageResult>() {
                @Override
                public AsyncDataController getData(
                        CancellationToken cancelToken,
                        AsyncDataListener<? super ImageResult> dataListener) {
                    // Never terminate.
                    return new SimpleDataController(new SimpleDataState("STARTED", 0.0));
                }
            };
        }
    }

    private static class TestImagePointTransformer implements ImagePointTransformer {
        private final Point2D.Double input;
        private final Point2D.Double output;

        public TestImagePointTransformer(Point2D.Double input, Point2D.Double output) {
            this.input = new Point2D.Double(input.x, input.y);
            this.output = new Point2D.Double(output.x, output.y);
        }

        @Override
        public void transformSrcToDest(Point2D src, Point2D dest) {
            assertEquals(input, src);
            dest.setLocation(output.x, output.y);
        }

        @Override
        public void transformDestToSrc(Point2D dest, Point2D src) {
            assertEquals(output, dest);
            src.setLocation(input.x, input.y);
        }
    }

    private static class RawTransformedImageDisplay extends TransformedImageDisplay<TestInput> {
        private static final long serialVersionUID = 9021655555745732700L;

        public RawTransformedImageDisplay() {
            this(null);
        }

        public RawTransformedImageDisplay(AsyncRendererFactory asyncRenderer) {
            super(asyncRenderer);
        }
    }

    private static class TransformedImageDisplayImpl extends TransformedImageDisplay<TestInput> {
        private static final long serialVersionUID = -3279074017352087265L;
        public final TransformationStepDef firstStep;

        public TransformedImageDisplayImpl() {
            this(null);
        }

        public TransformedImageDisplayImpl(AsyncRendererFactory asyncRenderer) {
            super(asyncRenderer);
            this.firstStep = addFirstStep();
        }
    }

    private static class FillWithTestTransformationStep implements ImageTransformationStep {
        private final ImagePointTransformer pointTransformer;

        public FillWithTestTransformationStep(ImagePointTransformer pointTransformer) {
            this.pointTransformer = pointTransformer;
        }

        @Override
        public TransformedImage render(
                CancellationToken cancelToken,
                TransformationStepInput input,
                BufferedImage offeredBuffer) {
            BufferedImage testImg = createTestImage(
                    input.getDestinationWidth(),
                    input.getDestinationHeight());

            ImageTransformationStep step = createTransformation(testImg, pointTransformer);
            return step.render(cancelToken, input, offeredBuffer);
        }
    }
}
