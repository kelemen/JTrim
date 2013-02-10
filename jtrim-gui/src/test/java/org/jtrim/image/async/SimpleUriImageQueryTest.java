package org.jtrim.image.async;

import java.net.URI;
import java.net.URISyntaxException;
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