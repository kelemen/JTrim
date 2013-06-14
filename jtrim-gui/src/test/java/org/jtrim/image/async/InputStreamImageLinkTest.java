package org.jtrim.image.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.io.InputStreamOpener;
import org.jtrim.image.ImageResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class InputStreamImageLinkTest {
    private static final double ALLOWED_INTERMEDIATE_RATIO = 0.5;

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

    private static InputStreamOpener createPathInputStreamOpener(final Path file) {
        return new InputStreamOpener() {
            @Override
            public InputStream openStream(CancellationToken cancelToken) throws IOException {
                return Files.newInputStream(file);
            }
        };
    }

    private static ImageIOLinkFactory createStandardLinkFactory() {
        return new ImageIOLinkFactory() {
            @Override
            public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor) {
                InputStreamOpener streamOpener = createPathInputStreamOpener(file);
                return new InputStreamImageLink(executor, streamOpener, ALLOWED_INTERMEDIATE_RATIO);
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
    /**
     * Test of toString method, of class InputStreamImageLink.
     */
    @Test
    public void testToString() {
        String streamName = "InputStreamImageLink.testToString";
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        InputStreamOpener streamOpener = mock(InputStreamOpener.class);
        stub(streamOpener.toString()).toReturn(streamName);

        InputStreamImageLink link = new InputStreamImageLink(
                executor, streamOpener, 0.0);

        String strValue = link.toString();
        assertNotNull(strValue);
        assertTrue("toString() must contain the stream name.", strValue.contains(streamName));
    }
}
