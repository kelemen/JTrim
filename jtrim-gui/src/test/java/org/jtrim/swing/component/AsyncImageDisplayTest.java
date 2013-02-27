package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncLinks;
import org.jtrim.concurrent.async.DataConverter;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.ImageTransformerData;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.image.transform.TransformedImageData;
import org.jtrim.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.jtrim.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncImageDisplayTest {
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
    public void testSetInputInSingleCall() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncImageDisplay<TestInput> component) {
                    component.setImageQuery(createTestQuery(), new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(test.getCurrentContent());
                }
            });
        }
    }

    @Test
    public void testSetInputInSeparateCall() {
        try (final TestCase test = TestCase.create()) {
            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncImageDisplay<TestInput> component) {
                    component.setImageQuery(createTestQuery());
                    component.setImageAddress(new TestImage(component));
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(test.getCurrentContent());
                }
            });
        }
    }

    private static ImageTransformerData captureTransformerArg(TestTransformation transf) {
        ArgumentCaptor<ImageTransformerData> arg = ArgumentCaptor.forClass(ImageTransformerData.class);
        verify(transf, atLeastOnce()).createDataLink(arg.capture());
        return arg.getValue();
    }

    @Test
    public void testUncachedTransformation() {
        try (final TestCase test = TestCase.create()) {
            final AtomicReference<TestTransformation> transf1Ref = new AtomicReference<>(null);
            final AtomicReference<TestTransformation> transf2Ref = new AtomicReference<>(null);

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncImageDisplay<TestInput> component) {
                    TestTransformation transf1 = spy(createTransformation(new ClearImage(component, Color.BLUE)));
                    TestTransformation transf2 = spy(createTransformation(new TestImage(component)));
                    transf1Ref.set(transf1);
                    transf2Ref.set(transf2);

                    component.setImageQuery(createTestQuery(), new ClearImage(component, Color.GREEN));
                    component.setImageTransformer(0, ReferenceType.NoRefType, transf1);
                    component.setImageTransformer(1, ReferenceType.NoRefType, transf2);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(test.getCurrentContent());

                    BufferedImage input1 = captureTransformerArg(transf1Ref.get()).getSourceImage();
                    checkImageContent(input1, Color.GREEN);

                    BufferedImage input2 = captureTransformerArg(transf2Ref.get()).getSourceImage();
                    checkImageContent(input2, Color.BLUE);
                }
            });
        }
    }

    @Test
    public void testCachedTransformation() {
        try (final TestCase test = TestCase.create()) {
            final AtomicReference<TestTransformation> transf1Ref = new AtomicReference<>(null);
            final AtomicReference<TestTransformation> transf2Ref = new AtomicReference<>(null);

            test.runTest(new TestMethod() {
                @Override
                public void run(AsyncImageDisplay<TestInput> component) {
                    TestTransformation transf1 = spy(createTransformation(new ClearImage(component, Color.BLUE)));
                    TestTransformation transf2 = spy(createTransformation(new TestImage(component)));
                    transf1Ref.set(transf1);
                    transf2Ref.set(transf2);

                    component.setImageQuery(createTestQuery(), new ClearImage(component, Color.GREEN));
                    component.setImageTransformer(0, ReferenceType.HardRefType, JavaRefObjectCache.INSTANCE, transf1);
                    component.setImageTransformer(1, ReferenceType.HardRefType, JavaRefObjectCache.INSTANCE, transf2);
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    test.runTest(new TestMethod() {
                        @Override
                        public void run(AsyncImageDisplay<TestInput> component) {
                            component.renderAgain();
                        }
                    });
                }
            });

            runAfterEvents(new Runnable() {
                @Override
                public void run() {
                    checkTestImagePixels(test.getCurrentContent());

                    BufferedImage input1 = captureTransformerArg(transf1Ref.get()).getSourceImage();
                    checkImageContent(input1, Color.GREEN);

                    BufferedImage input2 = captureTransformerArg(transf2Ref.get()).getSourceImage();
                    checkImageContent(input2, Color.BLUE);

                    // Verify that transformers has not been called multiple
                    // times.
                    verify(transf1Ref.get()).createDataLink(any(ImageTransformerData.class));
                    verify(transf2Ref.get()).createDataLink(any(ImageTransformerData.class));
                }
            });
        }
    }

    private static TestTransformation createTransformation(final TestInput input) {
        return new TestTransformation(input);
    }

    private static TestQuery createTestQuery() {
        return TestQuery.INSTANCE;
    }

    private static class TestTransformation implements AsyncDataQuery<ImageTransformerData, TransformedImageData> {
        private final TestInput input;

        public TestTransformation(TestInput input) {
            assert input != null;
            this.input = input;
        }

        @Override
        public AsyncDataLink<TransformedImageData> createDataLink(ImageTransformerData arg) {
            AsyncDataLink<ImageData> link = input.createLink();
            return AsyncLinks.convertResult(link, new DataConverter<ImageData, TransformedImageData>() {
                @Override
                public TransformedImageData convertData(ImageData data) {
                    TransformedImage transformed = new TransformedImage(data.getImage(), null);
                    return new TransformedImageData(transformed, null);
                }
            });
        }
    }

    private enum TestQuery implements AsyncDataQuery<TestInput, ImageData> {
        INSTANCE;

        @Override
        public AsyncDataLink<ImageData> createDataLink(TestInput arg) {
            return arg.createLink();
        }
    }

    public static final class TestCase implements AutoCloseable {
        private JFrame owner;
        private CapturePaintComponent parent;
        private AsyncImageDisplay<TestInput> component;

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
                public AsyncImageDisplay<TestInput> create() {
                    AsyncImageDisplay<TestInput> result = new AsyncImageDisplay<>(
                            new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor()));
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
        public AsyncImageDisplay<TestInput> create();
    }

    public static interface TestMethod {
        public void run(AsyncImageDisplay<TestInput> component) throws Throwable;
    }

    public static interface TestInput {
        public AsyncDataLink<ImageData> createLink();
    }

    public static final class ClearImage implements TestInput {
        private final int width;
        private final int height;
        private final Color color;

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
        }

        @Override
        public AsyncDataLink<ImageData> createLink() {
            BufferedImage testImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            fillImage(testImg, color);
            ImageMetaData metaData = new ImageMetaData(width, height, true);
            return AsyncLinks.createPreparedLink(new ImageData(testImg, metaData, null), null);
        }
    }

    public static final class TestImage implements TestInput {
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
        public AsyncDataLink<ImageData> createLink() {
            BufferedImage testImg = createTestImage(width, height);
            ImageMetaData metaData = new ImageMetaData(width, height, true);
            return AsyncLinks.createPreparedLink(new ImageData(testImg, metaData, null), null);
        }
    }
}
