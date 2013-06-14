package org.jtrim.image.async;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
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
public class UriImageIOQueryTest {
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


    private static ImageIOLinkFactory createStandardLinkFactory() {
        return new ImageIOLinkFactory() {
            @Override
            public AsyncDataLink<ImageResult> createLink(Path file, TaskExecutor executor) {
                UriImageIOQuery query = new UriImageIOQuery(executor, ALLOWED_INTERMEDIATE_RATIO);
                return query.createDataLink(file.toUri());
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

    @Test(expected = NullPointerException.class)
    public void testNullUri() throws Exception {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        UriImageIOQuery query = new UriImageIOQuery(executor, 1.0);
        query.createDataLink(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalUri() throws Exception {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        UriImageIOQuery query = new UriImageIOQuery(executor, 1.0);
        query.createDataLink(new URI("dir/file"));
    }

    /**
     * Test of toString method, of class SimpleUriImageQuery.
     */
    @Test
    public void testToString() {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        UriImageIOQuery query = new UriImageIOQuery(executor, 1.0);
        assertNotNull(query.toString());
    }
}
