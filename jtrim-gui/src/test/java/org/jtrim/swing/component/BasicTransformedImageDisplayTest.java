package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.image.transform.AffineImagePointTransformer;
import org.jtrim.image.transform.AffineTransformationStep;
import org.jtrim.image.transform.BasicImageTransformations;
import org.jtrim.image.transform.ImagePointTransformer;
import org.jtrim.image.transform.InterpolationType;
import org.jtrim.image.transform.ZoomToFitTransformationStep;
import org.jtrim.swing.component.TransformedImageDisplayTest.ClearImage;
import org.jtrim.swing.component.TransformedImageDisplayTest.ComponentFactory;
import org.jtrim.swing.component.TransformedImageDisplayTest.TestCaseGeneric;
import org.jtrim.swing.component.TransformedImageDisplayTest.TestInput;
import org.jtrim.swing.component.TransformedImageDisplayTest.TestMethodGeneric;
import org.jtrim.swing.concurrent.async.AsyncRenderer;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.DataRenderer;
import org.jtrim.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim.image.transform.PointTransformerChecks.checkEqualPointTransformers;
import static org.jtrim.image.transform.ZoomToFitOption.*;
import static org.jtrim.swing.component.GuiTestUtils.*;
import static org.jtrim.swing.component.TransformedImageDisplayTest.createTestQuery;
import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class BasicTransformedImageDisplayTest {
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
        runOnEDT(new Runnable() {
            @Override
            public void run() {
                BasicTransformedImageDisplay<TestInput> component
                        = new BasicTransformedImageDisplay<>();
                assertTrue(component.getTransformations().isIdentity());
                assertNull(component.getZoomToFitOptions());
                assertFalse(component.isInZoomToFitMode());
                assertFalse(component.alwaysClearZoomToFit().getValue());

                Point2D.Double input1 = new Point2D.Double(5.0, 6.0);
                assertEquals(input1, component.getPreAffinePoint((Point2D)input1.clone()));

                Point2D.Double input2 = new Point2D.Double(8.0, 7.0);
                assertEquals(input2, component.getPreAffinePoint((Point2D)input2.clone()));

                Point2D.Double input3 = new Point2D.Double(9.0, 4.0);
                assertEquals(input3, component.getDisplayPointFromPreAffinePoint((Point2D)input3.clone()));

                component.movePreAffinePointToDisplayPoint(
                        new Point2D.Double(0.0, 0.0),
                        new Point2D.Double(1000.0, 1000.0));
                assertTrue(component.getTransformations().isIdentity());
            }
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

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.setTransformations(transf);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(BasicTransformedImageDisplay<TestInput> component) {
                            double displayX = 10.0;
                            double displayY = 11.0;

                            component.movePreAffinePointToDisplayPoint(
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
                        }
                    });
                }
            });
        }
    }

    @Test
    public void testNonInvertible() {
        try (TestCase test = TestCase.create()) {
            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.setZoom(0.0);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.GREEN));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(BasicTransformedImageDisplay<TestInput> component) {
                            Point2D.Double displayPoint = new Point2D.Double();
                            try {
                                component.getPreAffinePoint(displayPoint);
                                fail("Expected: IllegalStateException");
                            } catch (IllegalStateException ex) {
                            }
                        }
                    });
                }
            });
        }
    }

    @Test
    public void testInterpolationTypes() {
        try (TestCase test = TestCase.create()) {
            final int imageWidth = 5;
            final int imageHeight = 6;

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.setZoomToFit(false, true, true, true);
                    component.interpolationType().setValue(InterpolationType.NEAREST_NEIGHBOR);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.GREEN));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage currentContent = test.getCurrentContent();
                    checkBlankImage(currentContent, Color.GREEN);
                }
            });

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight, Color.BLUE));
                    component.interpolationType().setValue(InterpolationType.BILINEAR);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    BufferedImage currentContent = test.getCurrentContent();
                    checkBlankImage(currentContent, Color.BLUE);
                }
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

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.setTransformations(transf);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                        }
                    });
                }
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
            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                }
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

            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.setTransformations(transf);
                    component.setZoomToFit(true, true);
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(imageWidth, imageHeight));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                        }
                    });
                }
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
            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                }
            });
        }
    }

    @Test
    public void testTransformationChanges() {
        try (TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                }
            });
        }
    }

    @Test
    public void testAutoClearZoomToFitLazy() {
        try (TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                private void enterZoomToFit(BasicTransformedImageDisplay<TestInput> component) {
                    component.setZoomToFit(true, true);
                    assertTrue(component.isInZoomToFitMode());
                }

                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                private void enterZoomToFit(BasicTransformedImageDisplay<TestInput> component) {
                    component.setZoomToFit(true, true);
                    assertTrue(component.isInZoomToFitMode());
                }

                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
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
                }
            });
        }
    }

    @Test
    public void testToStringOfCompleteQuery() {
        final Collection<String> linkStrValues = new ConcurrentLinkedQueue<>();
        BasicComponentFactory factory = new BasicComponentFactory() {
            @Override
            public BasicTransformedImageDisplay<TestInput> create() {
                final AsyncRendererFactory wrappedRenderer
                        = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

                BasicTransformedImageDisplay<TestInput> result = new BasicTransformedImageDisplay<>();
                result.setAsyncRenderer(new AsyncRendererFactory() {
                    @Override
                    public AsyncRenderer createRenderer() {
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
                    }
                });
                return result;
            }
        };
        try (final TestCase test = TestCase.create(factory)) {
            test.runTest(new TestMethod() {
                @Override
                public void run(BasicTransformedImageDisplay<TestInput> component) {
                    component.imageQuery().setValue(createTestQuery());
                    component.imageAddress().setValue(new ClearImage(7, 8, Color.BLACK));
                    component.setZoomToFit(true, true);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    for (String strValues: linkStrValues) {
                        assertNotNull(strValues);
                    }
                }
            });
        }
    }

    private static final class TestCase extends TestCaseGeneric<BasicTransformedImageDisplay<TestInput>>  {
        public static TestCase create() {
            return create(new ComponentFactory<BasicTransformedImageDisplay<TestInput>>() {
                @Override
                public BasicTransformedImageDisplay<TestInput> create() {
                    BasicTransformedImageDisplay<TestInput> result = new BasicTransformedImageDisplay<>(
                            new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
                    return result;
                }
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

    private static interface TestMethod extends TestMethodGeneric<BasicTransformedImageDisplay<TestInput>> {
    }
}
