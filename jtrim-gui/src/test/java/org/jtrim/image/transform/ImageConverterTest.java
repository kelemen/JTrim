package org.jtrim.image.transform;

import org.jtrim.concurrent.async.DataConverter;
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
public class ImageConverterTest {
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

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockDataConverter() {
        return mock(DataConverter.class);
    }

    /**
     * Test of convertData method, of class ImageConverter.
     */
    @Test
    public void testConvertData() {
        DataConverter<ImageTransformerData, TransformedImage> wrapped = mockDataConverter();
        ImageConverter converter = new ImageConverter(wrapped);

        ImageTransformerData input = new ImageTransformerData(null, 5, 6, null);
        TransformedImage output = new TransformedImage(null, null);

        stub(wrapped.convertData(any(ImageTransformerData.class))).toReturn(output);

        TransformedImageData convertedData = converter.convertData(input);
        assertSame(output, convertedData.getTransformedImage());
        assertNull(convertedData.getException());

        verify(wrapped).convertData(same(input));
    }

    /**
     * Test of toString method, of class ImageConverter.
     */
    @Test
    public void testToString() {
        DataConverter<ImageTransformerData, TransformedImage> wrapped = mockDataConverter();
        ImageConverter converter = new ImageConverter(wrapped);
        assertNotNull(converter.toString());
    }
}
