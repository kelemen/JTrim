package org.jtrim2.image;

import java.util.Arrays;
import javax.imageio.metadata.IIOMetadata;
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
public class JavaIIOMetaDataTest {
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

    private static JavaIIOMetaData create(int width, int height, IIOMetadata metaData, boolean complete) {
        return new JavaIIOMetaData(width, height, metaData, complete);
    }

    @Test
    public void testProperties() {
        for (int width: Arrays.asList(0, 1, 56)) {
            for (int height: Arrays.asList(0, 1, 37)) {
                for (boolean complete: Arrays.asList(false, true)) {
                    for (IIOMetadata wrapped: Arrays.asList(null, mock(IIOMetadata.class))) {
                        JavaIIOMetaData metaData = create(width, height, wrapped, complete);
                        assertEquals(width, metaData.getWidth());
                        assertEquals(height, metaData.getHeight());
                        assertEquals(complete, metaData.isComplete());
                        assertSame(wrapped, metaData.getIioMetaData());
                    }
                }
            }
        }
    }
}
