package org.jtrim2.image.async;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.async.AsyncDataController;
import org.jtrim2.concurrent.async.AsyncDataLink;
import org.jtrim2.concurrent.async.AsyncDataListener;
import org.jtrim2.concurrent.async.AsyncReport;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.image.ImageMetaData;
import org.jtrim2.image.ImageResult;
import org.jtrim2.image.JavaIIOMetaData;
import org.jtrim2.utils.ExceptionHelper;

import static org.jtrim2.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class StandardImageQueryTests {
    private static final int TEST_IMG_WIDTH = 8;
    private static final int TEST_IMG_HEIGHT = 9;

    private final ImageIOLinkFactory imageQuery;

    public StandardImageQueryTests(ImageIOLinkFactory imageQuery) {
        ExceptionHelper.checkNotNullArgument(imageQuery, "imageQuery");
        this.imageQuery = imageQuery;
    }

    private AsyncDataLink<ImageResult> create(Path file, TaskExecutor executor) {
        return imageQuery.createLink(file, executor);
    }

    private static ImageResultVerifier combineVerifiers(ImageResultVerifier... verifiers) {
        final ImageResultVerifier[] currentVerifiers = verifiers.clone();
        return (long numberOfImages, ImageResult lastResult, AsyncReport report) -> {
            for (ImageResultVerifier verifier: currentVerifiers) {
                verifier.verifyImageResult(numberOfImages, lastResult, report);
            }
        };
    }

    private enum SuccessVerifier implements ImageResultVerifier {
        INSTANCE;

        @Override
        public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report) {
            assertTrue("Must have received an image.", numberOfImages > 0);
            assertTrue("Unexpected report: " + report.toString(), report.isSuccess());

            BufferedImage lastImage = lastResult.getImage();
            assertEquals(TEST_IMG_WIDTH, lastImage.getWidth());
            assertEquals(TEST_IMG_HEIGHT, lastImage.getHeight());
            assertEquals(TEST_IMG_WIDTH, lastResult.getMetaData().getWidth());
            assertEquals(TEST_IMG_HEIGHT, lastResult.getMetaData().getHeight());
            assertTrue(lastResult.getMetaData().isComplete());
            assertTrue(lastResult.getMetaData() instanceof JavaIIOMetaData);

            checkTestImagePixels(lastResult.getImage());
        }
    }

    private enum CanceledVerifier implements ImageResultVerifier {
        INSTANCE;

        @Override
        public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report) {
            assertTrue("Unexpected report: " + report.toString(), report.isCanceled());
            assertTrue(report.getException() == null
                    || report.getException() instanceof OperationCanceledException);

            if (lastResult != null) {
                ImageMetaData metaData = lastResult.getMetaData();
                assertNotNull("meta-data should not be null if image has been received.", metaData);
                assertFalse(
                        "The last image should nto be complete on successful cancel",
                        lastResult.getMetaData().isComplete());

                BufferedImage lastImage = lastResult.getImage();
                if (lastImage != null) {
                    assertEquals(TEST_IMG_WIDTH, lastImage.getWidth());
                    assertEquals(TEST_IMG_HEIGHT, lastImage.getHeight());
                }
                assertEquals(TEST_IMG_WIDTH, lastResult.getMetaData().getWidth());
                assertEquals(TEST_IMG_HEIGHT, lastResult.getMetaData().getHeight());
            }
        }
    }

    private static final class FailedVerifier implements ImageResultVerifier {
        private final Class<? extends Throwable> expectedException;

        public FailedVerifier(Class<? extends Throwable> expectedException) {
            ExceptionHelper.checkNotNullArgument(expectedException, "expectedException");
            this.expectedException = expectedException;
        }

        @Override
        public void verifyImageResult(long numberOfImages, ImageResult lastResult, AsyncReport report) {
            assertNotNull(report.getException());

            Class<?> receviedType = report.getException().getClass();
            assertTrue("Invalid exception type: " + receviedType,
                    expectedException.isAssignableFrom(receviedType));
        }
    }

    private static void testGetImage(String format, GetImageTest test) throws Throwable {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jtrim", ".test");
            BufferedImage testImage = createTestImageWithoutAlpha(TEST_IMG_WIDTH, TEST_IMG_HEIGHT);
            ImageIO.write(testImage, format, tempFile.toFile());

            test.testGetImage(tempFile);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private void testGetImage(String format, final ImageResultVerifier... verifiers) throws Throwable {
        final ImageResultVerifier verifier = combineVerifiers(
                SuccessVerifier.INSTANCE,
                combineVerifiers(verifiers));

        testGetImage(format, (Path file) -> {
            final ContextAwareTaskExecutor taskExecutor
                    = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());

            ImageCollectorListener listener = new ImageCollectorListener(taskExecutor);

            AsyncDataLink<ImageResult> link = create(file, taskExecutor);
            AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
            assertNotNull(controller.getDataState());
            controller.controlData(null);

            AsyncReport report = listener.getReport();
            ImageResult lastResult = listener.getLastResult();
            long imageCount = listener.getImageCount();

            listener.verifyNoTrivialErrors();
            assertNotNull("AsyncReport cannot be null.", report);

            verifier.verifyImageResult(imageCount, lastResult, report);
        });
    }

    public void testGetImagePng(ImageResultVerifier... verifiers) throws Throwable {
        testGetImage("png", verifiers);
    }

    public void testGetImageBmp(ImageResultVerifier... verifiers) throws Throwable {
        testGetImage("bmp", verifiers);
    }

    private void testGetImageCanceledWhileRetrieving(
            String format,
            final ImageResultVerifier... verifiers) throws Throwable {
        testGetImage(format, (Path file) -> {
            final ContextAwareTaskExecutor taskExecutor
                    = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());

            final CancellationSource cancelSource = Cancellation.createCancellationSource();
            final AtomicBoolean expectSuccess = new AtomicBoolean(false);
            AsyncDataListener<ImageResult> wrapped = new AsyncDataListener<ImageResult>() {
                @Override
                public void onDataArrive(ImageResult data) {
                    cancelSource.getController().cancel();
                    expectSuccess.set(data.getMetaData().isComplete());
                }

                @Override
                public void onDoneReceive(AsyncReport report) {
                }
            };

            AsyncDataLink<ImageResult> link = create(file, taskExecutor);

            ImageCollectorListener listener = new ImageCollectorListener(taskExecutor, wrapped);
            AsyncDataController controller = link.getData(cancelSource.getToken(), listener);
            assertNotNull(controller.getDataState());
            controller.controlData(null);

            AsyncReport report = listener.getReport();
            ImageResult lastResult = listener.getLastResult();
            long imageCount = listener.getImageCount();

            listener.verifyNoTrivialErrors();

            if (expectSuccess.get()) {
                SuccessVerifier.INSTANCE.verifyImageResult(imageCount, lastResult, report);
            }
            else {
                ImageResultVerifier verifier = combineVerifiers(
                        CanceledVerifier.INSTANCE,
                        combineVerifiers(verifiers));
                verifier.verifyImageResult(imageCount, lastResult, report);
            }
        });
    }

    public void testGetImageCanceledWhileRetrievingPng(ImageResultVerifier... verifiers) throws Throwable {
        testGetImageCanceledWhileRetrieving("png", verifiers);
    }

    public void testGetImageCanceledWhileRetrievingBmp(ImageResultVerifier... verifiers) throws Throwable {
        testGetImageCanceledWhileRetrieving("bmp", verifiers);
    }

    public void testGetImageCanceledBeforeRetrieving(final ImageResultVerifier... verifiers) throws Throwable {
        testGetImage("bmp", (Path file) -> {
            final ContextAwareTaskExecutor taskExecutor
                    = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
            AsyncDataLink<ImageResult> link = create(file, taskExecutor);
            ImageCollectorListener listener = new ImageCollectorListener(taskExecutor);

            AsyncDataController controller = link.getData(Cancellation.CANCELED_TOKEN, listener);
            assertNotNull(controller.getDataState());
            controller.controlData(null);

            AsyncReport report = listener.getReport();
            ImageResult lastResult = listener.getLastResult();
            long imageCount = listener.getImageCount();

            listener.verifyNoTrivialErrors();

            assertEquals("Should not have received an image.", 0L, imageCount);

            ImageResultVerifier verifier = combineVerifiers(
                    CanceledVerifier.INSTANCE,
                    combineVerifiers(verifiers));
            verifier.verifyImageResult(imageCount, lastResult, report);
        });
    }

    public void testInvalidFormat(ImageResultVerifier... verifiers) throws IOException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jtrim", ".test");

            final ContextAwareTaskExecutor taskExecutor
                    = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());

            AsyncDataLink<ImageResult> link = create(tempFile, taskExecutor);
            ImageCollectorListener listener = new ImageCollectorListener(taskExecutor);
            AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
            assertNotNull(controller.getDataState());
            controller.controlData(null);

            AsyncReport report = listener.getReport();
            ImageResult lastResult = listener.getLastResult();
            long imageCount = listener.getImageCount();

            listener.verifyNoTrivialErrors();

            assertEquals("Should not have received an image.", 0L, imageCount);

            ImageResultVerifier verifier = combineVerifiers(
                    new FailedVerifier(Throwable.class),
                    combineVerifiers(verifiers));
            verifier.verifyImageResult(imageCount, lastResult, report);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    public void testUnreadableFile(ImageResultVerifier... verifiers) throws Exception {
        Path testPath = Paths.get(
                "this-should-not-exist",
                "really",
                "please",
                "dont",
                "create",
                "this",
                "directory",
                "tree");

        final ContextAwareTaskExecutor taskExecutor
                = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
        AsyncDataLink<ImageResult> link = create(testPath, taskExecutor);
        ImageCollectorListener listener = new ImageCollectorListener(taskExecutor);

        AsyncDataController controller = link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);
        assertNotNull(controller.getDataState());
        controller.controlData(null);

        AsyncReport report = listener.getReport();
        ImageResult lastResult = listener.getLastResult();
        long imageCount = listener.getImageCount();

        listener.verifyNoTrivialErrors();

        assertEquals("Should not have received an image.", 0L, imageCount);

        ImageResultVerifier verifier = combineVerifiers(
                new FailedVerifier(Throwable.class),
                combineVerifiers(verifiers));
        verifier.verifyImageResult(imageCount, lastResult, report);
    }

    private static final class ImageCollectorListener implements AsyncDataListener<ImageResult> {
        private final Queue<String> errors;
        private final ContextAwareTaskExecutor taskExecutor;
        private final AtomicReference<ImageResult> lastResultRef;
        private final AtomicLong numberOfImages;
        private final AtomicReference<AsyncReport> reportRef;
        private final AtomicBoolean finished;

        private final AsyncDataListener<ImageResult> delegate;

        public ImageCollectorListener(ContextAwareTaskExecutor taskExecutor) {
            this(taskExecutor, null);
        }

        public ImageCollectorListener(
                ContextAwareTaskExecutor taskExecutor,
                AsyncDataListener<ImageResult> delegate) {
            this.errors = new ConcurrentLinkedQueue<>();
            this.taskExecutor = taskExecutor;
            this.lastResultRef = new AtomicReference<>(null);
            this.numberOfImages = new AtomicLong(0);
            this.reportRef = new AtomicReference<>(null);
            this.finished = new AtomicBoolean(false);
            this.delegate = delegate;
        }

        @Override
        public void onDataArrive(ImageResult data) {
            if (delegate != null) {
                delegate.onDataArrive(data);
            }

            if (finished.get()) {
                errors.add("onDataArrive has been called after onDoneReceive.");
            }

            if (!taskExecutor.isExecutingInThis()) {
                errors.add("onDataArrive must be executed from the context of the executor.");
            }

            lastResultRef.set(data);
            numberOfImages.incrementAndGet();
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            if (delegate != null) {
                delegate.onDoneReceive(report);
            }

            if (finished.getAndSet(true)) {
                errors.add("onDoneReceive has been called multiple times.");
            }

            if (!taskExecutor.isExecutingInThis()) {
                errors.add("onDoneReceive must be executed from the context of the executor.");
            }

            reportRef.set(report);
        }

        public void verifyNoTrivialErrors() {
            Object[] receivedErrors = errors.toArray();
            assertTrue(Arrays.toString(receivedErrors), receivedErrors.length == 0);
        }

        public AsyncReport getReport() {
            return reportRef.get();
        }

        public long getImageCount() {
            return numberOfImages.get();
        }

        public ImageResult getLastResult() {
            return lastResultRef.get();
        }
    }

    private static interface GetImageTest {
        public void testGetImage(Path file) throws Throwable;
    }
}
