package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.AsyncDataConverter;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.DataConverter;
import org.jtrim.image.ImageMetaData;
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
@SuppressWarnings("deprecation")
public class ImageTransformerLinkTest {
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
    public void testConversion1() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            TestCase inputs = new TestCase(numberOfTransformers);
            inputs.runTest1();
        }
    }

    @Test
    public void testConversion2() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            TestCase inputs = new TestCase(numberOfTransformers);
            inputs.runTest2();
        }
    }

    @Test
    public void testConversion3() {
        for (int numberOfTransformers = 1; numberOfTransformers < 4; numberOfTransformers++) {
            TestCase inputs = new TestCase(numberOfTransformers);
            inputs.runTest3();
        }
    }

    static final class TestCase {
        private final ImageMetaData metaData;
        private final BufferedImage image0;
        private final ImageTransformerData input;

        private final int numberOfTransformers;
        private final ImageTransformer[] transformers;
        private final TransformedImage[] outputs;
        private final BufferedImage[] outputImages;
        private final ImagePointTransformer[] outputTransformers;
        private volatile ContextChecker contextChecker;
        private final AtomicReference<String> contextError;

        public TestCase(int numberOfTransformers) {
            this.numberOfTransformers = numberOfTransformers;

            metaData = new ImageMetaData(7, 8, true);
            image0 = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
            input = new ImageTransformerData(image0, 8, 9, metaData);
            contextError = new AtomicReference<>(null);
            contextChecker = new ContextChecker() {
                @Override
                public void checkContext(int transformerIndex) {
                    contextError.set("Missing context checker.");
                }
            };

            transformers = new ImageTransformer[numberOfTransformers];
            outputs = new TransformedImage[numberOfTransformers];
            outputImages = new BufferedImage[numberOfTransformers];
            outputTransformers = new ImagePointTransformer[numberOfTransformers];
            for (int i = 0; i < numberOfTransformers; i++) {
                transformers[i] = mock(ImageTransformer.class);
                outputImages[i] = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
                outputTransformers[i] = mock(ImagePointTransformer.class);
                outputs[i] = new TransformedImage(outputImages[i], outputTransformers[i]);

                final int currentIndex = i;
                stub(transformers[i].convertData(any(ImageTransformerData.class)))
                        .toAnswer(new Answer<TransformedImage>() {
                    @Override
                    public TransformedImage answer(InvocationOnMock invocation) {
                        contextChecker.checkContext(currentIndex);
                        return outputs[currentIndex];
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> AsyncDataListener<T> mockListener() {
            return mock(AsyncDataListener.class);
        }

        private void verifyLink(ImageTransformerLink link) {
            AsyncDataListener<TransformedImageData> listener = mockListener();
            assertNotNull(link.getData(Cancellation.UNCANCELABLE_TOKEN, listener));

            InOrder inOrderListener = inOrder(listener);
            ArgumentCaptor<TransformedImageData> arg = ArgumentCaptor.forClass(TransformedImageData.class);
            inOrderListener.verify(listener, times(numberOfTransformers)).onDataArrive(arg.capture());
            inOrderListener.verify(listener).onDoneReceive(any(AsyncReport.class));
            inOrderListener.verifyNoMoreInteractions();

            TransformedImageData[] receivedArgs = arg.getAllValues().toArray(new TransformedImageData[0]);
            for (int i = 0; i < receivedArgs.length; i++) {
                assertSame(outputImages[i], receivedArgs[i].getImage());
                assertSame(outputTransformers[i], receivedArgs[i].getPointTransformer());
            }

            InOrder inOrderTransformers = inOrder((Object[])transformers);
            for (int i = 0; i < transformers.length; i++) {
                ArgumentCaptor<ImageTransformerData> transfArg = ArgumentCaptor.forClass(ImageTransformerData.class);
                inOrderTransformers.verify(transformers[i]).convertData(transfArg.capture());
                ImageTransformerData receivedTransfArg = transfArg.getValue();

                assertSame(image0, receivedTransfArg.getSourceImage());
                assertSame(metaData, receivedTransfArg.getMetaData());
            }
            inOrderTransformers.verifyNoMoreInteractions();

            assertNull(contextError.get(), contextError.get());
            assertNotNull(link.toString());
        }

        // May only run one of the tests.

        public void runTest1() {
            runTest1(new LinkFactory1() {
                @Override
                public ImageTransformerLink createLink(
                        ImageTransformerData input,
                        TaskExecutorService executor,
                        ImageTransformer[] transformers) {
                    return new ImageTransformerLink(input, executor, transformers);
                }
            });
        }

        public void runTest1(LinkFactory1 factory) {
            final SyncTaskExecutor executor = new SyncTaskExecutor();
            contextChecker = new ContextChecker() {
                @Override
                public void checkContext(int transformerIndex) {
                    if (!executor.isExecutingInThis()) {
                        contextError.set("Not running in the context of the executor.");
                    }
                }
            };

            ImageTransformerLink link = factory.createLink(input, executor, transformers);
            verifyLink(link);
        }

        public void runTest2() {
            runTest2(new LinkFactory2() {
                @Override
                public ImageTransformerLink createLink(
                        ImageTransformerData input,
                        List<AsyncDataConverter<ImageTransformerData, TransformedImage>> asyncConverters) {
                    return new ImageTransformerLink(input, asyncConverters);
                }
            });
        }

        public void runTest2(LinkFactory2 factory) {
            List<AsyncDataConverter<ImageTransformerData, TransformedImage>> asyncConverters
                    = new ArrayList<>(numberOfTransformers);
            final SyncTaskExecutor[] executors = new SyncTaskExecutor[numberOfTransformers];

            for (int i = 0; i < numberOfTransformers; i++) {
                executors[i] = new SyncTaskExecutor();
                asyncConverters.add(new AsyncDataConverter<>(transformers[i], executors[i]));
            }

            contextChecker = new ContextChecker() {
                @Override
                public void checkContext(int transformerIndex) {
                    if (!executors[transformerIndex].isExecutingInThis()) {
                        contextError.set("Not running in the context of the executor.");
                    }
                }
            };

            ImageTransformerLink link = factory.createLink(input, asyncConverters);
            verifyLink(link);
        }

        public void runTest3() {
            List<AsyncDataConverter<ImageTransformerData, TransformedImageData>> asyncConverters
                    = new ArrayList<>(numberOfTransformers);
            final SyncTaskExecutor[] executors = new SyncTaskExecutor[numberOfTransformers];

            for (int i = 0; i < numberOfTransformers; i++) {
                executors[i] = new SyncTaskExecutor();

                final int currentIndex = i;
                DataConverter<ImageTransformerData, TransformedImageData> converter
                        = new DataConverter<ImageTransformerData, TransformedImageData>() {
                    @Override
                    public TransformedImageData convertData(ImageTransformerData data) {
                        TransformedImage result = transformers[currentIndex].convertData(data);
                        return new TransformedImageData(result, null);
                    }
                };
                AsyncDataConverter<ImageTransformerData, TransformedImageData> asyncConverter
                        = new AsyncDataConverter<>(converter, executors[i]);
                asyncConverters.add(asyncConverter);
            }

            contextChecker = new ContextChecker() {
                @Override
                public void checkContext(int transformerIndex) {
                    if (!executors[transformerIndex].isExecutingInThis()) {
                        contextError.set("Not running in the context of the executor.");
                    }
                }
            };

            ImageTransformerLink link = ImageTransformerLink.createFromDataTransformers(input, asyncConverters);
            verifyLink(link);
        }
    }

    static interface LinkFactory1 {
        public ImageTransformerLink createLink(
                ImageTransformerData input,
                TaskExecutorService executor,
                ImageTransformer[] transformers);
    }

    static interface LinkFactory2 {
        public ImageTransformerLink createLink(
                ImageTransformerData input,
                List<AsyncDataConverter<ImageTransformerData, TransformedImage>> asyncConverters);
    }

    private static interface ContextChecker {
        public void checkContext(int transformerIndex);
    }
}
