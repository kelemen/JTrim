package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JFrame;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.image.transform.AffineImagePointTransformer;
import org.jtrim2.image.transform.AffineTransformationStep;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ImagePointTransformer;
import org.jtrim2.image.transform.InterpolationType;
import org.jtrim2.image.transform.ZoomToFitTransformationStep;
import org.jtrim2.swing.component.AsyncImageDisplayTest.ClearImage;
import org.jtrim2.swing.component.AsyncImageDisplayTest.TestInput;
import org.jtrim2.swing.concurrent.async.AsyncRenderer;
import org.jtrim2.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim2.swing.concurrent.async.DataRenderer;
import org.jtrim2.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim2.swing.concurrent.async.RenderingState;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.image.transform.PointTransformerChecks.*;
import static org.jtrim2.image.transform.ZoomToFitOption.*;
import static org.jtrim2.swing.component.AsyncImageDisplayTest.*;
import static org.jtrim2.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("deprecation")
public class SimpleAsyncImageDisplayTest {
    private static final double DOUBLE_TOLERANCE = 0.00000001;

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
    public void testInitialProperties() {
        runOnEDT(() -> {
            SimpleAsyncImageDisplay<TestInput> component
                    = new SimpleAsyncImageDisplay<>();
            assertTrue(component.getTransformations().isIdentity());
            assertNull(component.getZoomToFitOptions());
            assertFalse(component.isInZoomToFitMode());
            assertFalse(component.isAlwaysClearZoomToFit());

            Point2D.Double input1 = new Point2D.Double(5.0, 6.0);
            assertEquals(input1, component.getImagePoint((Point2D)input1.clone()));

            Point2D.Double input2 = new Point2D.Double(8.0, 7.0);
            assertEquals(input2, component.getImagePoint((Point2D)input2.clone()));

            Point2D.Double input3 = new Point2D.Double(9.0, 4.0);
            assertEquals(input3, component.getDisplayPoint((Point2D)input3.clone()));

            component.moveImagePointToDisplayPoint(
                    new Point2D.Double(0.0, 0.0),
                    new Point2D.Double(1000.0, 1000.0));
            assertTrue(component.getTransformations().isIdentity());
        });
    }

    private static ImagePointTransformer getComponentPointTransformer(
            final SimpleAsyncImageDisplay<?> component) {
        return new ImagePointTransformer() {
            @Override
            public void transformSrcToDest(Point2D src, Point2D dest) {
                dest.setLocation(component.getDisplayPoint(src));
            }

            @Override
            public void transformDestToSrc(Point2D dest, Point2D src) {
                src.setLocation(component.getImagePoint(dest));
            }
        };
    }

    @Test
    public void testMoveImagePointToDisplayPoint() {
        try (TestCase test = TestCase.create()) {
            BasicImageTransformations.Builder transfBuilder
                    = new BasicImageTransformations.Builder();
            transfBuilder.setOffset(20.0, 30.0);
            transfBuilder.setZoom(2.0);
            transfBuilder.setRotateInDegrees(90);
            final BasicImageTransformations transf = transfBuilder.create();

            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.setImageQuery(
                        AsyncImageDisplayTest.createTestQuery(),
                        new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                    double displayX = 10.0;
                    double displayY = 11.0;

                    component.moveImagePointToDisplayPoint(
                            new Point2D.Double(0.0, 0.0),
                            new Point2D.Double(displayX, displayY));

                    double expectedOffsetX
                            = -(double)component.getWidth() / 2.0
                            - (double)imageHeight + displayX;
                    double expectedOffsetY
                            = -(double)component.getHeight() / 2.0
                            + (double)imageWidth + displayY;

                    BasicImageTransformations newTransf = component.getTransformations();
                    assertEquals(transf.getRotateInDegrees(), newTransf.getRotateInDegrees());
                    assertEquals(transf.getZoomX(), newTransf.getZoomX(), DOUBLE_TOLERANCE);
                    assertEquals(transf.getZoomY(), newTransf.getZoomY(), DOUBLE_TOLERANCE);
                    assertEquals(expectedOffsetX, newTransf.getOffsetX(), DOUBLE_TOLERANCE);
                    assertEquals(expectedOffsetY, newTransf.getOffsetY(), DOUBLE_TOLERANCE);
                    assertEquals(transf.isFlipHorizontal(), newTransf.isFlipHorizontal());
                    assertEquals(transf.isFlipVertical(), newTransf.isFlipVertical());
                });
            });
        }
    }

    @Test
    public void testNonInvertible() {
        try (TestCase test = TestCase.create()) {
            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setZoom(0.0);
                component.setImageQuery(
                        AsyncImageDisplayTest.createTestQuery(),
                        new ClearImage(imageWidth, imageHeight, Color.GREEN));
            });

            runAfterEvents(() -> {
                test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                    Point2D.Double displayPoint = new Point2D.Double();
                    try {
                        component.getImagePoint(displayPoint);
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                    }
                });
            });
        }
    }

    @Test
    public void testInterpolationTypes() {
        try (TestCase test = TestCase.create()) {
            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setZoomToFit(false, true, true, true);
                component.setInterpolationTypes(InterpolationType.NEAREST_NEIGHBOR);
                component.setImageQuery(
                        AsyncImageDisplayTest.createTestQuery(),
                        new ClearImage(imageWidth, imageHeight, Color.GREEN));
            });

            runAfterEvents(() -> {
                BufferedImage currentContent = test.getCurrentContent();
                checkBlankImage(currentContent, Color.GREEN);
            });

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setImageAddress(
                        new ClearImage(imageWidth, imageHeight, Color.BLUE));
                component.setInterpolationTypes(
                        InterpolationType.NEAREST_NEIGHBOR,
                        InterpolationType.BILINEAR,
                        InterpolationType.BICUBIC);
            });

            runAfterEvents(() -> {
                BufferedImage currentContent = test.getCurrentContent();
                checkBlankImage(currentContent, Color.BLUE);
            });
        }
    }

    @Test
    public void testAfterDisplay() {
        try (TestCase test = TestCase.create()) {
            BasicImageTransformations.Builder transfBuilder
                    = new BasicImageTransformations.Builder();
            transfBuilder.setFlipHorizontal(false);
            transfBuilder.setFlipVertical(true);
            transfBuilder.setOffset(10.0, 11.0);
            transfBuilder.setZoomX(12.0);
            transfBuilder.setZoomY(13.0);
            transfBuilder.setRotateInRadians(Math.PI / 9.0);
            final BasicImageTransformations transf = transfBuilder.create();

            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.setImageQuery(
                        AsyncImageDisplayTest.createTestQuery(),
                        new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                    AffineTransform affineTransf = AffineTransformationStep.getTransformationMatrix(
                            transf,
                            imageWidth,
                            imageHeight,
                            component.getWidth(),
                            component.getHeight());

                    AffineImagePointTransformer expectedPointTransf
                            = new AffineImagePointTransformer(affineTransf);

                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.getDisplayedPointTransformer());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.getPointTransformer());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            getComponentPointTransformer(component));
                });
            });

            // Test that getPointTransformer is immediately calculated after
            // property change.
            BasicImageTransformations.Builder transfBuilder2
                    = new BasicImageTransformations.Builder();
            transfBuilder2.setFlipHorizontal(true);
            transfBuilder2.setFlipVertical(false);
            transfBuilder2.setOffset(20.0, 21.0);
            transfBuilder2.setZoomX(22.0);
            transfBuilder2.setZoomY(23.0);
            transfBuilder2.setRotateInRadians(Math.PI / 6.0);
            final BasicImageTransformations transf2 = transfBuilder2.create();
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setTransformations(transf2);
                AffineTransform affineTransf = AffineTransformationStep.getTransformationMatrix(
                        transf2,
                        imageWidth,
                        imageHeight,
                        component.getWidth(),
                        component.getHeight());

                AffineImagePointTransformer expectedPointTransf
                        = new AffineImagePointTransformer(affineTransf);

                checkEqualPointTransformers(
                        expectedPointTransf,
                        component.getPointTransformer());
                checkEqualPointTransformers(
                        expectedPointTransf,
                        getComponentPointTransformer(component));
            });
        }
    }

    @Test
    public void testZoomToFitAfterDisplay() {
        try (TestCase test = TestCase.create()) {
            BasicImageTransformations.Builder transfBuilder
                    = new BasicImageTransformations.Builder();
            transfBuilder.setFlipHorizontal(false);
            transfBuilder.setFlipVertical(true);
            transfBuilder.setOffset(10.0, 11.0);
            transfBuilder.setZoomX(12.0);
            transfBuilder.setZoomY(13.0);
            transfBuilder.setRotateInRadians(Math.PI / 9.0);
            final BasicImageTransformations transf = transfBuilder.create();

            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.setZoomToFit(true, true);
                component.setImageQuery(
                        AsyncImageDisplayTest.createTestQuery(),
                        new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                    BasicImageTransformations appliedTransf;
                    appliedTransf = ZoomToFitTransformationStep.getBasicTransformations(
                            imageWidth,
                            imageHeight,
                            component.getWidth(),
                            component.getHeight(),
                            component.getZoomToFitOptions(),
                            transf);
                    AffineTransform affineTransf = AffineTransformationStep.getTransformationMatrix(
                            appliedTransf,
                            imageWidth,
                            imageHeight,
                            component.getWidth(),
                            component.getHeight());
                    AffineImagePointTransformer expectedPointTransf
                            = new AffineImagePointTransformer(affineTransf);

                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.getDisplayedPointTransformer());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.getPointTransformer());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            getComponentPointTransformer(component));
                });
            });

            // Test that getPointTransformer is immediately calculated after
            // property change.
            BasicImageTransformations.Builder transfBuilder2
                    = new BasicImageTransformations.Builder();
            transfBuilder2.setFlipHorizontal(true);
            transfBuilder2.setFlipVertical(false);
            transfBuilder2.setOffset(20.0, 21.0);
            transfBuilder2.setZoomX(22.0);
            transfBuilder2.setZoomY(23.0);
            transfBuilder2.setRotateInRadians(Math.PI / 6.0);
            final BasicImageTransformations transf2 = transfBuilder2.create();
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setTransformations(transf2);
                component.setZoomToFit(true, true);

                BasicImageTransformations appliedTransf;
                appliedTransf = ZoomToFitTransformationStep.getBasicTransformations(
                        imageWidth,
                        imageHeight,
                        component.getWidth(),
                        component.getHeight(),
                        component.getZoomToFitOptions(),
                        transf2);
                AffineTransform affineTransf = AffineTransformationStep.getTransformationMatrix(
                        appliedTransf,
                        imageWidth,
                        imageHeight,
                        component.getWidth(),
                        component.getHeight());
                AffineImagePointTransformer expectedPointTransf
                        = new AffineImagePointTransformer(affineTransf);

                checkEqualPointTransformers(
                        expectedPointTransf,
                        component.getPointTransformer());
                checkEqualPointTransformers(
                        expectedPointTransf,
                        getComponentPointTransformer(component));
            });
        }
    }

    @Test
    public void testExecutors() {
        try (TestCase test = TestCase.create()) {
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                SyncTaskExecutor executor0 = new SyncTaskExecutor();
                component.setDefaultExecutor(executor0);
                assertSame(executor0, component.getDefaultExecutor());
                for (InterpolationType interpolation: InterpolationType.values()) {
                    assertSame(executor0, component.getExecutor(interpolation));
                }

                SyncTaskExecutor executor1 = new SyncTaskExecutor();
                component.setExecutor(InterpolationType.NEAREST_NEIGHBOR, executor1);
                assertSame(executor1, component.getExecutor(InterpolationType.NEAREST_NEIGHBOR));

                SyncTaskExecutor executor2 = new SyncTaskExecutor();
                component.setExecutor(InterpolationType.BILINEAR, executor2);
                assertSame(executor2, component.getExecutor(InterpolationType.BILINEAR));

                SyncTaskExecutor executor3 = new SyncTaskExecutor();
                component.setExecutor(InterpolationType.BICUBIC, executor3);
                assertSame(executor3, component.getExecutor(InterpolationType.BICUBIC));

                component.setExecutor(InterpolationType.NEAREST_NEIGHBOR, null);
                assertSame(executor0, component.getExecutor(InterpolationType.NEAREST_NEIGHBOR));
            });
        }
    }

    @Test
    public void testTransformationChanges() {
        try (TestCase test = TestCase.create()) {
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setRotateInDegrees(80);
                assertEquals(80, component.getTransformations().getRotateInDegrees());

                double rotateRad = Math.PI / 6.0;

                component.setRotateInRadians(rotateRad);
                component.setZoomX(2.0);
                component.setZoomY(3.0);
                component.setFlipHorizontal(true);
                component.setOffset(4.0, 5.0);

                BasicImageTransformations transf;

                transf = component.getTransformations();
                assertEquals(rotateRad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);
                assertEquals(2.0, transf.getZoomX(), DOUBLE_TOLERANCE);
                assertEquals(3.0, transf.getZoomY(), DOUBLE_TOLERANCE);
                assertEquals(4.0, transf.getOffsetX(), DOUBLE_TOLERANCE);
                assertEquals(5.0, transf.getOffsetY(), DOUBLE_TOLERANCE);
                assertTrue(transf.isFlipHorizontal());
                assertFalse(transf.isFlipVertical());

                component.setZoom(6.0);
                component.setFlipHorizontal(false);
                component.setFlipVertical(true);

                transf = component.getTransformations();
                assertEquals(rotateRad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomX(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomY(), DOUBLE_TOLERANCE);
                assertEquals(4.0, transf.getOffsetX(), DOUBLE_TOLERANCE);
                assertEquals(5.0, transf.getOffsetY(), DOUBLE_TOLERANCE);
                assertFalse(transf.isFlipHorizontal());
                assertTrue(transf.isFlipVertical());

                component.flipVertical();

                transf = component.getTransformations();
                assertEquals(rotateRad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomX(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomY(), DOUBLE_TOLERANCE);
                assertEquals(4.0, transf.getOffsetX(), DOUBLE_TOLERANCE);
                assertEquals(5.0, transf.getOffsetY(), DOUBLE_TOLERANCE);
                assertFalse(transf.isFlipHorizontal());
                assertFalse(transf.isFlipVertical());
                assertFalse(component.isInZoomToFitMode());

                component.flipHorizontal();

                transf = component.getTransformations();
                assertEquals(rotateRad, transf.getRotateInRadians(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomX(), DOUBLE_TOLERANCE);
                assertEquals(6.0, transf.getZoomY(), DOUBLE_TOLERANCE);
                assertEquals(4.0, transf.getOffsetX(), DOUBLE_TOLERANCE);
                assertEquals(5.0, transf.getOffsetY(), DOUBLE_TOLERANCE);
                assertTrue(transf.isFlipHorizontal());
                assertFalse(transf.isFlipVertical());
                assertFalse(component.isInZoomToFitMode());

                component.setZoomToFit(true, false);
                assertTrue(component.isInZoomToFitMode());
                assertEquals(
                        EnumSet.of(KEEP_ASPECT_RATIO, FIT_HEIGHT, FIT_WIDTH),
                        component.getZoomToFitOptions());

                component.setZoomToFit(false, true);
                assertTrue(component.isInZoomToFitMode());
                assertEquals(
                        EnumSet.of(MAY_MAGNIFY, FIT_HEIGHT, FIT_WIDTH),
                        component.getZoomToFitOptions());

                component.setZoomToFit(false, true, false, true);
                assertTrue(component.isInZoomToFitMode());
                assertEquals(
                        EnumSet.of(MAY_MAGNIFY, FIT_HEIGHT),
                        component.getZoomToFitOptions());

                component.setZoomToFit(false, true, true, false);
                assertTrue(component.isInZoomToFitMode());
                assertEquals(
                        EnumSet.of(MAY_MAGNIFY, FIT_WIDTH),
                        component.getZoomToFitOptions());

                component.clearZoomToFit();
                assertFalse(component.isInZoomToFitMode());
                assertNull(component.getZoomToFitOptions());

                component.setDefaultTransformations();
                assertTrue(component.getTransformations().isIdentity());
                assertNull(component.getZoomToFitOptions());
                assertFalse(component.isInZoomToFitMode());
                assertFalse(component.isAlwaysClearZoomToFit());

                BasicImageTransformations.Builder newTransfBuilder
                        = new BasicImageTransformations.Builder();
                newTransfBuilder.setFlipHorizontal(false);
                newTransfBuilder.setFlipVertical(true);
                newTransfBuilder.setOffset(10.0, 11.0);
                newTransfBuilder.setZoomX(12.0);
                newTransfBuilder.setZoomY(13.0);
                newTransfBuilder.setRotateInRadians(Math.PI / 9.0);
                BasicImageTransformations newTransf = newTransfBuilder.create();

                component.setZoomToFit(true, true);
                component.setTransformations(newTransf);

                transf = component.getTransformations();
                assertEquals(newTransf.getRotateInRadians(), transf.getRotateInRadians(), 0.0);
                assertEquals(newTransf.getZoomX(), transf.getZoomX(), 0.0);
                assertEquals(newTransf.getZoomY(), transf.getZoomY(), 0.0);
                assertEquals(newTransf.getOffsetX(), transf.getOffsetX(), 0.0);
                assertEquals(newTransf.getOffsetY(), transf.getOffsetY(), 0.0);
                assertEquals(newTransf.isFlipHorizontal(), transf.isFlipHorizontal());
                assertEquals(newTransf.isFlipVertical(), transf.isFlipVertical());
                assertFalse(component.isInZoomToFitMode());
            });
        }
    }

    @Test
    public void testAutoClearZoomToFitLazy() {
        try (TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                private void enterZoomToFit(SimpleAsyncImageDisplay<TestInput> component) {
                    component.setZoomToFit(true, true);
                    assertTrue(component.isInZoomToFitMode());
                }

                @Override
                public void run(SimpleAsyncImageDisplay<TestInput> component) {
                    enterZoomToFit(component);

                    component.setRotateInDegrees(80);
                    assertTrue(component.isInZoomToFitMode());

                    component.setRotateInRadians(Math.PI / 9.0);
                    assertTrue(component.isInZoomToFitMode());

                    component.flipHorizontal();
                    assertTrue(component.isInZoomToFitMode());

                    component.flipVertical();
                    assertTrue(component.isInZoomToFitMode());

                    component.setFlipHorizontal(false);
                    assertTrue(component.isInZoomToFitMode());

                    component.setFlipVertical(false);
                    assertTrue(component.isInZoomToFitMode());

                    component.setZoom(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setZoomX(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setZoomY(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setOffset(1.0, 0.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);
                }
            });
        }
    }

    @Test
    public void testAutoClearZoomToFitAlways() {
        try (TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                private void enterZoomToFit(SimpleAsyncImageDisplay<TestInput> component) {
                    component.setZoomToFit(true, true);
                    assertTrue(component.isInZoomToFitMode());
                }

                @Override
                public void run(SimpleAsyncImageDisplay<TestInput> component) {
                    component.setAlwaysClearZoomToFit(true);

                    enterZoomToFit(component);

                    component.setRotateInDegrees(80);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setRotateInRadians(Math.PI / 9.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.flipHorizontal();
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.flipVertical();
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setFlipHorizontal(true);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setFlipVertical(true);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setZoom(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setZoomX(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setZoomY(1.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);

                    component.setOffset(1.0, 0.0);
                    assertFalse(component.isInZoomToFitMode());
                    enterZoomToFit(component);
                }
            });
        }
    }

    @Test
    public void testListeners() {
        try (TestCase test = TestCase.create()) {
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                TransformationListener transfListener = mock(TransformationListener.class);
                component.addTransformationListener(transfListener);

                component.setZoom(2.0);
                verify(transfListener).zoomChanged();

                component.setZoomX(3.0);
                verify(transfListener, times(2)).zoomChanged();

                component.setZoomY(4.0);
                verify(transfListener, times(3)).zoomChanged();

                component.setOffset(5.0, 6.0);
                verify(transfListener).offsetChanged();

                component.setFlipHorizontal(true);
                verify(transfListener).flipChanged();

                component.setFlipVertical(true);
                verify(transfListener, times(2)).flipChanged();

                component.flipHorizontal();
                verify(transfListener, times(3)).flipChanged();

                component.flipVertical();
                verify(transfListener, times(4)).flipChanged();

                component.setRotateInDegrees(80);
                verify(transfListener).rotateChanged();

                double rotateRad = Math.PI / 6.0;
                component.setRotateInRadians(rotateRad);
                verify(transfListener, times(2)).rotateChanged();

                // NO-OP changes
                component.setZoomX(3.0);
                component.setZoomY(4.0);
                component.setOffset(5.0, 6.0);
                component.setFlipHorizontal(false);
                component.setFlipVertical(false);
                verify(transfListener, times(3)).zoomChanged();
                verify(transfListener).offsetChanged();
                verify(transfListener, times(4)).flipChanged();
                verify(transfListener, times(2)).rotateChanged();

                // Zoom to fit changes.
                component.setZoomToFit(true, false, false, false);
                verify(transfListener).enterZoomToFitMode(eq(EnumSet.of(KEEP_ASPECT_RATIO)));

                component.clearZoomToFit();
                verify(transfListener).leaveZoomToFitMode();

                verifyNoMoreInteractions(transfListener);
            });
        }
    }

    @Test
    public void testToStringOfCompleteQuery() {
        final Collection<String> linkStrValues = new ConcurrentLinkedQueue<>();
        ComponentFactory factory = () -> {
            final AsyncRendererFactory wrappedRenderer
                    = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

            SimpleAsyncImageDisplay<TestInput> result = new SimpleAsyncImageDisplay<>();
            result.setAsyncRenderer(() -> {
                final AsyncRenderer wrapped = wrappedRenderer.createRenderer();
                return new AsyncRenderer() {
                    @Override
                    public <DataType> RenderingState render(
                            CancellationToken cancelToken,
                            AsyncDataLink<DataType> dataLink,
                            DataRenderer<? super DataType> renderer) {
                        linkStrValues.add(dataLink.toString());
                        return wrapped.render(cancelToken, dataLink, renderer);
                    }
                };
            });
            return result;
        };
        try (final TestCase test = TestCase.create(factory)) {
            test.runTest((SimpleAsyncImageDisplay<TestInput> component) -> {
                component.setImageQuery(createTestQuery(), new ClearImage(7, 8, Color.BLACK));
                component.setZoomToFit(true, true);
            });

            runAfterEvents(() -> {
                for (String strValues: linkStrValues) {
                    assertNotNull(strValues);
                }
            });
        }
    }

    public static final class TestCase implements AutoCloseable {
        private JFrame owner;
        private CapturePaintComponent parent;
        private SimpleAsyncImageDisplay<TestInput> component;

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
                SimpleAsyncImageDisplay<TestInput> result = new SimpleAsyncImageDisplay<>(
                        new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
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
        public SimpleAsyncImageDisplay<TestInput> create();
    }

    public static interface TestMethod {
        public void run(SimpleAsyncImageDisplay<TestInput> component) throws Throwable;
    }
}
