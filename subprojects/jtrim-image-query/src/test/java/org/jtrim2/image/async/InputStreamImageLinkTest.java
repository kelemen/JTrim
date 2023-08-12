package org.jtrim2.image.async;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.query.io.InputStreamOpener;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InputStreamImageLinkTest {
    private static final double ALLOWED_INTERMEDIATE_RATIO = 0.5;

    private static InputStreamOpener createPathInputStreamOpener(final Path file) {
        return (CancellationToken cancelToken) -> Files.newInputStream(file);
    }

    private static ImageIOLinkFactory createStandardLinkFactory() {
        return (Path file, TaskExecutor executor) -> {
            InputStreamOpener streamOpener = createPathInputStreamOpener(file);
            return new InputStreamImageLink(executor, streamOpener, ALLOWED_INTERMEDIATE_RATIO);
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
        when(streamOpener.toString()).thenReturn(streamName);

        InputStreamImageLink link = new InputStreamImageLink(
                executor, streamOpener, 0.0);

        String strValue = link.toString();
        assertNotNull(strValue);
        assertTrue("toString() must contain the stream name.", strValue.contains(streamName));
    }
}
