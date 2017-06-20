package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.image.transform.AffineImagePointTransformer;
import org.jtrim2.image.transform.AffineTransformationStep;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ImagePointTransformer;
import org.jtrim2.image.transform.InterpolationType;
import org.jtrim2.image.transform.ZoomToFitTransformationStep;
import org.jtrim2.swing.component.TransformedImageDisplayTest.ClearImage;
import org.jtrim2.swing.component.TransformedImageDisplayTest.ComponentFactory;
import org.jtrim2.swing.component.TransformedImageDisplayTest.NullImage;
import org.jtrim2.swing.component.TransformedImageDisplayTest.TestCaseGeneric;
import org.jtrim2.swing.component.TransformedImageDisplayTest.TestInput;
import org.jtrim2.ui.concurrent.query.AsyncRenderer;
import org.jtrim2.ui.concurrent.query.AsyncRendererFactory;
import org.jtrim2.ui.concurrent.query.DataRenderer;
import org.jtrim2.ui.concurrent.query.GenericAsyncRendererFactory;
import org.jtrim2.ui.concurrent.query.RenderingState;
import org.junit.Test;

import static org.jtrim2.image.transform.ZoomToFitOption.*;
import static org.jtrim2.swing.component.TransformedImageDisplayTest.*;
import static org.jtrim2.testutils.image.transform.PointTransformerChecks.*;
import static org.jtrim2.testutils.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class BasicTransformedImageDisplayTest {
    private static final double DOUBLE_TOLERANCE = 0.00000001;

    @Test
    public void testInitialProperties() {
        runOnEDT(() -> {
            BasicTransformedImageDisplay<TestInput> component
                    = new BasicTransformedImageDisplay<>();
            assertTrue(component.getTransformations().isIdentity());
            assertNull(component.getZoomToFitOptions());
            assertFalse(component.isInZoomToFitMode());
            assertFalse(component.alwaysClearZoomToFit().getValue());

            Point2D.Double input1 = new Point2D.Double(5.0, 6.0);
            assertEquals(input1, component.getPreAffinePoint((Point2D) input1.clone()));

            Point2D.Double input2 = new Point2D.Double(8.0, 7.0);
            assertEquals(input2, component.getPreAffinePoint((Point2D) input2.clone()));

            Point2D.Double input3 = new Point2D.Double(9.0, 4.0);
            assertEquals(input3, component.getDisplayPointFromPreAffinePoint((Point2D) input3.clone()));

            component.movePreAffinePointToDisplayPoint(
                    new Point2D.Double(0.0, 0.0),
                    new Point2D.Double(1000.0, 1000.0));
            assertTrue(component.getTransformations().isIdentity());
        });
    }

    private static ImagePointTransformer getComponentPointTransformer(
            final BasicTransformedImageDisplay<?> component) {
        return new ImagePointTransformer() {
            @Override
            public void transformSrcToDest(Point2D src, Point2D dest) {
                dest.setLocation(component.getDisplayPointFromPreAffinePoint(src));
            }

            @Override
            public void transformDestToSrc(Point2D dest, Point2D src) {
                src.setLocation(component.getPreAffinePoint(dest));
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

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                    double displayX = 10.0;
                    double displayY = 11.0;

                    component.movePreAffinePointToDisplayPoint(
                            new Point2D.Double(0.0, 0.0),
                            new Point2D.Double(displayX, displayY));

                    double expectedOffsetX
                            = -(double) component.getWidth() / 2.0
                            - (double) imageHeight + displayX;
                    double expectedOffsetY
                            = -(double) component.getHeight() / 2.0
                            + (double) imageWidth + displayY;

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
    public void testBackgroundChangeWithTransformation() {
        try (final TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setBackground(Color.BLUE);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(1, 1, Color.GREEN));
            });

            runAfterEvents(() -> {
                test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                    component.setBackground(Color.GREEN);
                });
            });

            runAfterEvents(() -> {
                checkBlankImage(test.getCurrentContent(), Color.GREEN);
            });
        }
    }

    @Test
    public void testNonInvertible() {
        try (TestCase test = TestCase.create()) {
            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setZoom(0.0);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.GREEN));
            });

            runAfterEvents(() -> {
                test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                    Point2D.Double displayPoint = new Point2D.Double();
                    try {
                        component.getPreAffinePoint(displayPoint);
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

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setZoomToFit(false, true, true, true);
                component.interpolationType().setValue(InterpolationType.NEAREST_NEIGHBOR);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.GREEN));
            });

            runAfterEvents(() -> {
                BufferedImage currentContent = test.getCurrentContent();
                checkBlankImage(currentContent, Color.GREEN);
            });

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.BLUE));
                component.interpolationType().setValue(InterpolationType.BILINEAR);
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

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
                            component.displayedPointTransformer().getValue());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.affinePointTransformer().getValue());
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
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
                        component.affinePointTransformer().getValue());
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

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.setTransformations(transf);
                component.setZoomToFit(true, true);
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
            });

            runAfterEvents(() -> {
                test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
                            component.displayedPointTransformer().getValue());
                    checkEqualPointTransformers(
                            expectedPointTransf,
                            component.affinePointTransformer().getValue());
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
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
                        component.affinePointTransformer().getValue());
                checkEqualPointTransformers(
                        expectedPointTransf,
                        getComponentPointTransformer(component));
            });
        }
    }

    @Test
    public void testTransformationChanges() {
        try (TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
                assertFalse(component.alwaysClearZoomToFit().getValue());

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
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
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
            });
        }
    }

    private static void enterZoomToFit(BasicTransformedImageDisplay<TestInput> component) {
        component.setZoomToFit(true, true);
        assertTrue(component.isInZoomToFitMode());
    }

    @Test
    public void testAutoClearZoomToFitAlways() {
        try (TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.alwaysClearZoomToFit().setValue(true);

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
            });
        }
    }

    @Test
    public void testToStringOfCompleteQuery() {
        final Collection<String> linkStrValues = new ConcurrentLinkedQueue<>();
        BasicComponentFactory factory = () -> {
            final AsyncRendererFactory wrappedRenderer
                    = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

            BasicTransformedImageDisplay<TestInput> result = new BasicTransformedImageDisplay<>();
            result.setAsyncRenderer((AsyncRendererFactory) () -> {
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
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(7, 8, Color.BLACK));
                component.setZoomToFit(true, true);
            });

            runAfterEvents(() -> {
                for (String strValues: linkStrValues) {
                    assertNotNull(strValues);
                }
            });
        }
    }

    @Test
    public void testGetAffineTransformationPos() {
        try (final TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(7, 8, Color.BLACK));
                component.getAffineTransformationPos()
                        .addAfter()
                        .setTransformation(createBlankTransformation(Color.GREEN));
            });

            runAfterEvents(() -> {
                BufferedImage currentContent = test.getCurrentContent();
                checkBlankImage(currentContent, Color.GREEN);
            });
        }
    }

    @Test
    public void testAffinePointTransformer() {
        try (final TestCase test = TestCase.create()) {
            final Runnable listener1 = mock(Runnable.class);
            final AtomicReference<ListenerRef> listenerRef = new AtomicReference<>(null);

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                listenerRef.set(component.affinePointTransformer().addChangeListener(listener1));

                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(7, 8, Color.BLACK));
            });

            runAfterEvents(() -> {
                verify(listener1).run();
                listenerRef.get().unregister();
            });

            final Runnable listener2 = mock(Runnable.class);
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.affinePointTransformer().addChangeListener(listener2);
                component.setRotateInDegrees(20);
            });

            runAfterEvents(() -> {
                verifyNoMoreInteractions(listener1);
                verify(listener2).run();
            });

            final Runnable listener3 = mock(Runnable.class);
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.affinePointTransformer().addChangeListener(listener3);
                component.setSize(component.getWidth() + 1, component.getHeight());
            });

            runAfterEvents(() -> {
                verify(listener3).run();
            });
        }
    }

    @Test
    public void testAffinePointTransformerAfterRevetingToNullImage() {
        try (final TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                component.imageQuery().setValue(createTestQuery());
                component.imageAddress().setValue(new ClearImage(7, 8, Color.BLACK));
            });

            waitAllSwingEvents();

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                assertNotNull(component.affinePointTransformer().getValue());
                component.imageAddress().setValue(new NullImage());
            });

            waitAllSwingEvents();

            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                assertNotNull(component.affinePointTransformer().getValue());
            });
        }
    }

    @Test
    public void testBasicTransformationProperty() {
        try (TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                BasicTransformationProperty properties = component.transformations();

                component.setZoomToFit(false, false, false, true);
                assertEquals(EnumSet.of(FIT_HEIGHT), properties.zoomToFit().getValue());

                component.setRotateInDegrees(30);
                assertEquals(30, properties.rotateInDegrees().getValue().intValue());

                component.setRotateInRadians(3.0);
                assertEquals(3.0, properties.rotateInRadians().getValue(), 0.0);

                component.setZoomX(4.0);
                assertEquals(4.0, properties.zoomX().getValue(), 0.0);

                component.setZoomY(5.0);
                assertEquals(5.0, properties.zoomY().getValue(), 0.0);

                component.setOffset(6.0, 7.0);
                assertEquals(6.0, properties.offsetX().getValue(), 0.0);
                assertEquals(7.0, properties.offsetY().getValue(), 0.0);

                for (boolean flip: Arrays.asList(true, false)) {
                    component.setFlipHorizontal(flip);
                    assertEquals(flip, properties.flipHorizontal().getValue());
                }

                for (boolean flip: Arrays.asList(true, false)) {
                    component.setFlipVertical(flip);
                    assertEquals(flip, properties.flipVertical().getValue());
                }
            });
        }
    }

    @Test
    public void testListeners() {
        try (TestCase test = TestCase.create()) {
            test.runTest((BasicTransformedImageDisplay<TestInput> component) -> {
                TransformationListener transfListener = mock(TransformationListener.class);
                component.addAffineTransformationListener(transfListener);

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

    private static final class TestCase extends TestCaseGeneric<BasicTransformedImageDisplay<TestInput>>  {
        public static TestCase create() {
            return create(() -> {
                BasicTransformedImageDisplay<TestInput> result = new BasicTransformedImageDisplay<>(
                        new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                return result;
            });
        }

        public static TestCase create(ComponentFactory<BasicTransformedImageDisplay<TestInput>> factory) {
            TestCase result = new TestCase();
            init(result, factory);
            return result;
        }
    }

    private static interface BasicComponentFactory
    extends
            ComponentFactory<BasicTransformedImageDisplay<TestInput>> {

    }
}
