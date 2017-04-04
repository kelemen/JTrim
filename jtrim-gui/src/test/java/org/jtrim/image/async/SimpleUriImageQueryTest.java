package org.jtrim.image.async;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
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
@SuppressWarnings("deprecation")
public class SimpleUriImageQueryTest {
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

    private static ImageIOLinkFactory createStandardLinkFactory() {
        return (Path file, TaskExecutor executor) -> {
            SimpleUriImageQuery query = new SimpleUriImageQuery(
                    executor, TimeUnit.MILLISECONDS.toNanos(200));
            return SimpleUriImageLinkTest.convertToStandard(query.createDataLink(file.toUri()));
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
    public void testCreateDataLink() throws URISyntaxException {
        URI uri = new URI("file:///dir/file");
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        long minUpdateTime = 3543643764L;

        SimpleUriImageQuery query = new SimpleUriImageQuery(executor, minUpdateTime);
        SimpleUriImageLink link = query.createDataLink(uri);

        assertEquals(uri, link.getImageUri());
        assertEquals(minUpdateTime, link.getMinUpdateTime(TimeUnit.NANOSECONDS));
    }

    /**
     * Test of toString method, of class SimpleUriImageQuery.
     */
    @Test
    public void testToString() {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        long minUpdateTime = 3543643764L;

        SimpleUriImageQuery query = new SimpleUriImageQuery(executor, minUpdateTime);
        assertNotNull(query.toString());
    }
}
