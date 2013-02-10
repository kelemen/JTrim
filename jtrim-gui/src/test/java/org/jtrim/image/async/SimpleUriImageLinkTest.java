package org.jtrim.image.async;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.ContextAwareTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.JavaIIOMetaData;
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
public class SimpleUriImageLinkTest {
    private static final long MIN_UPDATE_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(200);

    private static final int TEST_IMG_WIDTH = 8;
    private static final int TEST_IMG_HEIGHT = 9;
    private static final int[][] TEST_PIXELS;

    static {
        TEST_PIXELS = new int[TEST_IMG_HEIGHT][TEST_IMG_WIDTH];
        int pos = 0;
        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            @SuppressWarnings("MismatchedReadAndWriteOfArray")
            int[] line = TEST_PIXELS[y];
            int blue = (0xFF * y) / TEST_IMG_HEIGHT;

            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                int green = (0xFF * x) / TEST_IMG_WIDTH;
                int red = (0xFF * pos) / (TEST_IMG_WIDTH * TEST_IMG_HEIGHT);
                line[x] = blue | (green << 8) | (red << 16) | 0xFF00_0000;

                pos++;
            }
        }
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

    private static SimpleUriImageLink create(URI uri, TaskExecutor executor, long minUpdateTime) {
        return new SimpleUriImageLink(uri, executor, minUpdateTime);
    }

    private static SimpleUriImageLink create(URI uri, TaskExecutor executor) {
        return create(uri, executor, MIN_UPDATE_TIME_NANOS);
    }

    private static BufferedImage createTestImage() {
        BufferedImage bufferedImage = new BufferedImage(TEST_IMG_WIDTH, TEST_IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                bufferedImage.setRGB(x, y, TEST_PIXELS[y][x]);
            }
        }
        return bufferedImage;
    }


    private static void checkIfTestImage(BufferedImage image) {
        assertEquals(TEST_IMG_WIDTH, image.getWidth());
        assertEquals(TEST_IMG_HEIGHT, image.getHeight());

        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                int rgb = image.getRGB(x, y);
                assertTrue("Pixels must match.", rgb == TEST_PIXELS[y][x]);
            }
        }
    }

    private static void testGetImage(String format, GetImageTest test) throws Throwable {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jtrim", ".test");
            BufferedImage testImage = createTestImage();
            ImageIO.write(testImage, format, tempFile.toFile());

            test.testGetImage(tempFile.toUri());
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> AsyncDataListener<T> mockListener() {
        return mock(AsyncDataListener.class);
    }



    private static void verifySuccessfulReceive(AsyncDataListener<ImageData> mockedListener) throws Throwable {
        ArgumentCaptor<ImageData> imageDataArg = ArgumentCaptor.forClass(ImageData.class);
        ArgumentCaptor<AsyncReport> reportArg = ArgumentCaptor.forClass(AsyncReport.class);

        InOrder inOrder = inOrder(mockedListener);
        inOrder.verify(mockedListener, atLeastOnce()).onDataArrive(imageDataArg.capture());
        inOrder.verify(mockedListener).onDoneReceive(reportArg.capture());
        inOrder.verifyNoMoreInteractions();

        AsyncReport report = reportArg.getValue();
        if (report.getException() != null) {
            TestException toThrow = new TestException(report.getException());
            toThrow.addSuppressed(new AssertionError("Expected success but received: " + report));
            throw toThrow;
        }
        assertTrue(report.toString(), report.isSuccess());

        ImageData lastImage = imageDataArg.getValue();
        assertNull(lastImage.getException());
        assertEquals(TEST_IMG_WIDTH, lastImage.getWidth());
        assertEquals(TEST_IMG_HEIGHT, lastImage.getHeight());
        assertEquals(TEST_IMG_WIDTH, lastImage.getMetaData().getWidth());
        assertEquals(TEST_IMG_HEIGHT, lastImage.getMetaData().getHeight());
        assertTrue(lastImage.getMetaData().isComplete());
        assertTrue(lastImage.getMetaData() instanceof JavaIIOMetaData);

        checkIfTestImage(lastImage.getImage());
    }

    private void testGetImage(String format) throws Throwable {
        testGetImage(format, new GetImageTest() {
            @Override
            public void testGetImage(URI fileURI) throws Throwable {
                final ContextAwareTaskExecutor taskExecutor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
                SimpleUriImageLink link = create(fileURI, taskExecutor);
                AsyncDataListener<ImageData> listener = mockListener();

                final AtomicReference<String> errorRef = new AtomicReference<>(null);
                Answer<Void> checkContextAnswer = new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        if (!taskExecutor.isExecutingInThis()) {
                            errorRef.set("Must be executed from the context of the executor.");
                        }
                        return null;
                    }
                };
                doAnswer(checkContextAnswer).when(listener).onDataArrive(any(ImageData.class));
                doAnswer(checkContextAnswer).when(listener).onDoneReceive(any(AsyncReport.class));

                AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
                assertNotNull(controller.getDataState());
                controller.controlData(null);

                verifySuccessfulReceive(listener);
                assertNull(errorRef.get(), errorRef.get());
            }
        });
    }

    @Test
    public void testGetImagePng() throws Throwable {
        testGetImage("png");
    }

    @Test
    public void testGetImageBmp() throws Throwable {
        testGetImage("bmp");
    }

    private void testGetImageCanceledWhileRetrieving(String format) throws Throwable {
        testGetImage(format, new GetImageTest() {
            @Override
            public void testGetImage(URI fileURI) throws Throwable {
                final ContextAwareTaskExecutor taskExecutor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
                final CancellationSource cancelSource = Cancellation.createCancellationSource();
                SimpleUriImageLink link = create(fileURI, taskExecutor);
                AsyncDataListener<ImageData> listener = mockListener();

                final AtomicReference<String> errorRef = new AtomicReference<>(null);
                final Answer<Void> checkContextAnswer = new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        if (!taskExecutor.isExecutingInThis()) {
                            errorRef.set("Must be executed from the context of the executor.");
                        }
                        return null;
                    }
                };

                final AtomicBoolean expectSuccess = new AtomicBoolean(false);
                doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        cancelSource.getController().cancel();
                        ImageData arg = (ImageData)invocation.getArguments()[0];
                        expectSuccess.set(arg.getMetaData().isComplete());
                        return checkContextAnswer.answer(invocation);
                    }
                }).when(listener).onDataArrive(any(ImageData.class));
                doAnswer(checkContextAnswer).when(listener).onDoneReceive(any(AsyncReport.class));

                AsyncDataController controller = link.getData(cancelSource.getToken(), listener);
                assertNotNull(controller.getDataState());
                controller.controlData(null);

                if (expectSuccess.get()) {
                    verifySuccessfulReceive(listener);
                }
                else {
                    ArgumentCaptor<ImageData> imageDataArg = ArgumentCaptor.forClass(ImageData.class);
                    ArgumentCaptor<AsyncReport> reportArg = ArgumentCaptor.forClass(AsyncReport.class);
                    InOrder inOrder = inOrder(listener);
                    inOrder.verify(listener).onDataArrive(imageDataArg.capture());
                    inOrder.verify(listener).onDoneReceive(reportArg.capture());
                    inOrder.verifyNoMoreInteractions();

                    AsyncReport report = reportArg.getValue();
                    assertTrue(report.isCanceled());
                    assertTrue(report.getException() == null || report.getException() instanceof OperationCanceledException);

                    ImageData lastImage = imageDataArg.getValue();
                    ImageReceiveException exception = lastImage.getException();
                    assertTrue(exception == null || exception.getCause() instanceof OperationCanceledException);

                    assertEquals(TEST_IMG_WIDTH, lastImage.getWidth());
                    assertEquals(TEST_IMG_HEIGHT, lastImage.getHeight());
                    assertEquals(TEST_IMG_WIDTH, lastImage.getMetaData().getWidth());
                    assertEquals(TEST_IMG_HEIGHT, lastImage.getMetaData().getHeight());
                    assertFalse(lastImage.getMetaData().isComplete());
                    assertTrue(lastImage.getMetaData() instanceof JavaIIOMetaData);
                }
                assertNull(errorRef.get(), errorRef.get());
            }
        });
    }

    @Test
    public void testGetImageCanceledWhileRetrievingPng() throws Throwable {
        testGetImageCanceledWhileRetrieving("png");
    }

    @Test
    public void testGetImageCanceledWhileRetrievingBmp() throws Throwable {
        testGetImageCanceledWhileRetrieving("bmp");
    }

    @Test
    public void testGetImageCanceledBeforeRetrieving() throws Throwable {
        testGetImage("bmp", new GetImageTest() {
            @Override
            public void testGetImage(URI fileURI) {
                final ContextAwareTaskExecutor taskExecutor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
                SimpleUriImageLink link = create(fileURI, taskExecutor);
                AsyncDataListener<ImageData> listener = mockListener();

                final AtomicReference<String> errorRef = new AtomicReference<>(null);
                final Answer<Void> checkContextAnswer = new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        if (!taskExecutor.isExecutingInThis()) {
                            errorRef.set("Must be executed from the context of the executor.");
                        }
                        return null;
                    }
                };

                doAnswer(checkContextAnswer).when(listener).onDoneReceive(any(AsyncReport.class));

                AsyncDataController controller = link.getData(Cancellation.CANCELED_TOKEN, listener);
                assertNotNull(controller.getDataState());
                controller.controlData(null);

                ArgumentCaptor<AsyncReport> reportArg = ArgumentCaptor.forClass(AsyncReport.class);

                InOrder inOrder = inOrder(listener);
                inOrder.verify(listener, never()).onDataArrive(any(ImageData.class));
                inOrder.verify(listener).onDoneReceive(reportArg.capture());
                inOrder.verifyNoMoreInteractions();

                AsyncReport report = reportArg.getValue();
                assertTrue(report.isCanceled());
                assertTrue(report.getException() == null || report.getException() instanceof OperationCanceledException);
                assertNull(errorRef.get(), errorRef.get());
            }
        });
    }

    @Test
    public void testInvalidFormat() throws IOException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jtrim", ".test");

            final ContextAwareTaskExecutor taskExecutor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
            SimpleUriImageLink link = create(tempFile.toUri(), taskExecutor);
            AsyncDataListener<ImageData> listener = mockListener();

            final AtomicReference<String> errorRef = new AtomicReference<>(null);
            Answer<Void> checkContextAnswer = new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    if (!taskExecutor.isExecutingInThis()) {
                        errorRef.set("Must be executed from the context of the executor.");
                    }
                    return null;
                }
            };
            doAnswer(checkContextAnswer).when(listener).onDataArrive(any(ImageData.class));
            doAnswer(checkContextAnswer).when(listener).onDoneReceive(any(AsyncReport.class));

            AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
            assertNotNull(controller.getDataState());
            controller.controlData(null);

            ArgumentCaptor<ImageData> imageDataArg = ArgumentCaptor.forClass(ImageData.class);
            ArgumentCaptor<AsyncReport> reportArg = ArgumentCaptor.forClass(AsyncReport.class);
            InOrder inOrder = inOrder(listener);
            inOrder.verify(listener).onDataArrive(imageDataArg.capture());
            inOrder.verify(listener).onDoneReceive(reportArg.capture());
            inOrder.verifyNoMoreInteractions();

            ImageData imageData = imageDataArg.getValue();
            assertNull(imageData.getImage());
            assertNull(imageData.getMetaData());
            assertTrue(imageData.getWidth() < 0);
            assertTrue(imageData.getHeight() < 0);
            assertNotNull(imageData.getException());

            AsyncReport report = reportArg.getValue();
            assertFalse(report.isCanceled());
            assertNotNull(report.getException());
            assertNull(errorRef.get(), errorRef.get());
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @Test
    public void testUnreadableFile() throws Exception {
        URI testUri = new URI("file:///this-should-not-exist/really/please/dont/create/this/directory/tree");

        final ContextAwareTaskExecutor taskExecutor = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
        SimpleUriImageLink link = create(testUri, taskExecutor);
        AsyncDataListener<ImageData> listener = mockListener();

        final AtomicReference<String> errorRef = new AtomicReference<>(null);
        Answer<Void> checkContextAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                if (!taskExecutor.isExecutingInThis()) {
                    errorRef.set("Must be executed from the context of the executor.");
                }
                return null;
            }
        };
        doAnswer(checkContextAnswer).when(listener).onDataArrive(any(ImageData.class));
        doAnswer(checkContextAnswer).when(listener).onDoneReceive(any(AsyncReport.class));

        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
        assertNotNull(controller.getDataState());
        controller.controlData(null);

        ArgumentCaptor<ImageData> imageDataArg = ArgumentCaptor.forClass(ImageData.class);
        ArgumentCaptor<AsyncReport> reportArg = ArgumentCaptor.forClass(AsyncReport.class);
        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(imageDataArg.capture());
        inOrder.verify(listener).onDoneReceive(reportArg.capture());
        inOrder.verifyNoMoreInteractions();

        ImageData imageData = imageDataArg.getValue();
        assertNull(imageData.getImage());
        assertNull(imageData.getMetaData());
        assertTrue(imageData.getWidth() < 0);
        assertTrue(imageData.getHeight() < 0);
        assertNotNull(imageData.getException());

        AsyncReport report = reportArg.getValue();
        assertFalse(report.isCanceled());
        assertNotNull(report.getException());
        assertNull(errorRef.get(), errorRef.get());
    }

    @Test
    public void testGetImageUri() throws URISyntaxException {
        URI uri = new URI("file:///dir/file");
        SimpleUriImageLink link = create(uri, SyncTaskExecutor.getSimpleExecutor());
        assertEquals(uri, link.getImageUri());
    }

    @Test
    public void testGetMinUpdateTime() throws URISyntaxException {
        URI uri = new URI("file:///dir/file");
        long minUpdateTime = 5436437547L;
        SimpleUriImageLink link = create(uri, SyncTaskExecutor.getSimpleExecutor(), minUpdateTime);
        assertEquals(minUpdateTime, link.getMinUpdateTime(TimeUnit.NANOSECONDS));
    }


    @Test
    public void testToString() throws URISyntaxException {
        String strValue = create(new URI("file:///dir/file"), SyncTaskExecutor.getSimpleExecutor()).toString();
        assertNotNull(strValue);
    }

    private static interface GetImageTest {
        public void testGetImage(URI fileURI) throws Throwable;
    }

    private static final class TestException extends RuntimeException {
        private static final long serialVersionUID = -1920410239113432691L;

        public TestException(Throwable cause) {
            super(cause.toString(), cause, true, true);

            String testedClass = SimpleUriImageLink.class.getName();
            StackTraceElement[] stackTrace = cause.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement currentTrace = stackTrace[i];
                if (Objects.equals(currentTrace.getClassName(), testedClass)) {
                    stackTrace[i] = new StackTraceElement(
                            SimpleUriImageLinkTest.class.getName(),
                            currentTrace.getMethodName(),
                            "SimpleUriImageLinkTest.java",
                            currentTrace.getLineNumber());
                }
            }

            setStackTrace(stackTrace);
        }
    }
}