package org.jtrim.image.async;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageReceiveException;
import org.jtrim.image.ImageResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class SimpleUriImageLinkTest {
    private static final long MIN_UPDATE_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(200);

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

    public static AsyncDataLink<ImageResult> convertToStandard(final AsyncDataLink<ImageData> source) {
        return new AsyncDataLink<ImageResult>() {
            @Override
            public AsyncDataController getData(
                    CancellationToken cancelToken,
                    final AsyncDataListener<? super ImageResult> dataListener) {
                return source.getData(cancelToken, new AsyncDataListener<ImageData>() {
                    private ImageData lastData = null;

                    @Override
                    public void onDataArrive(ImageData data) {
                        lastData = data;
                        if (data.getImage() != null
                                || data.getMetaData() != null
                                || data.getException() == null) {
                            dataListener.onDataArrive(new ImageResult(data.getImage(), data.getMetaData()));
                        }
                    }

                    @Override
                    public void onDoneReceive(AsyncReport report) {
                        Throwable error = report.getException();
                        if (error instanceof IOException) {
                            ImageData data = lastData;
                            assertNotNull("The last sent data must contain a data with the failure.", data);

                            ImageReceiveException dataError = data.getException();
                            assertTrue("The last sent data must contain the failure",
                                    dataError != null && dataError.getCause() == error);
                            // Failing the above checks will cause the onDonReceive
                            // not to be called which will cause a test failure.
                        }
                        dataListener.onDoneReceive(report);
                    }
                });
            }
        };
    }

    private static ImageIOLinkFactory createStandardLinkFactory() {
        return new ImageIOLinkFactory() {
            @Override
            public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor) {
                AsyncDataLink<ImageData> link = new SimpleUriImageLink(
                        file.toUri(), executor, MIN_UPDATE_TIME_NANOS);
                return convertToStandard(link);
            }
        };
    }

    private static StandardImageQueryTests createStandardTester() {
        return new StandardImageQueryTests(createStandardLinkFactory());
    }

    @Test
    public void testGetImagePng() throws Throwable {
        createStandardTester().testGetImagePng();
    }

    @Test
    public void testGetImageBmp() throws Throwable {
        createStandardTester().testGetImageBmp();
    }

    @Test
    public void testGetImageCanceledWhileRetrievingPng() throws Throwable {
        createStandardTester().testGetImageCanceledWhileRetrievingPng();
    }

    @Test
    public void testGetImageCanceledWhileRetrievingBmp() throws Throwable {
        createStandardTester().testGetImageCanceledWhileRetrievingBmp();
    }

    @Test
    public void testGetImageCanceledBeforeRetrieving() throws Throwable {
        createStandardTester().testGetImageCanceledBeforeRetrieving();
    }

    @Test
    public void testInvalidFormat() throws IOException {
        createStandardTester().testInvalidFormat();
    }

    @Test
    public void testUnreadableFile() throws Exception {
        createStandardTester().testUnreadableFile();
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
}
