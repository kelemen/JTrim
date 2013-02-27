package org.jtrim.swing.concurrent.async;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import org.jtrim.swing.DelegateGraphics2D;
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
public class DrawingConnectorTest {
    private static final int[] STANDARD_BUFFER_TYPE = new int[]{
        BufferedImage.TYPE_3BYTE_BGR,
        BufferedImage.TYPE_4BYTE_ABGR,
        BufferedImage.TYPE_4BYTE_ABGR_PRE,
        BufferedImage.TYPE_BYTE_BINARY,
        BufferedImage.TYPE_BYTE_GRAY,
        BufferedImage.TYPE_BYTE_INDEXED,
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE,
        BufferedImage.TYPE_INT_BGR,
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_USHORT_555_RGB,
        BufferedImage.TYPE_USHORT_565_RGB,
        BufferedImage.TYPE_USHORT_GRAY
    };

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

    private static <T> DrawingConnector<T> create(int width, int height) {
        return new DrawingConnector<>(width, height);
    }

    @Test
    public void testInitialState() {
        for (int bufferType: STANDARD_BUFFER_TYPE) {
            for (int width: Arrays.asList(0, 1, 58)) {
                for (int height : Arrays.asList(0, 1, 37)) {
                    DrawingConnector<Object> connector = create(width, height);

                    BufferedImage image = connector.getDrawingSurface(bufferType);
                    assertEquals(bufferType, image.getType());
                    assertEquals(width != 0 ? width : 1, image.getWidth());
                    assertEquals(height != 0 ? height : 1, image.getHeight());
                }
            }
        }
    }

    @Test
    public void testDimensionAfterSet() {
        DrawingConnector<Object> connector = create(24, 32);
        connector.setRequiredWidth(39, 42);
        BufferedImage image = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertEquals(39, image.getWidth());
        assertEquals(42, image.getHeight());
    }

    private static void callGC() {
        System.gc();
        System.gc();
        System.runFinalization();
        System.gc();
        System.gc();
        System.runFinalization();
    }

    @Test
    public void testHasNewImage() {
        DrawingConnector<Object> connector = create(24, 32);
        assertFalse(connector.hasImage());

        assertTrue(connector.offerBuffer(new BufferedImage(24, 32, BufferedImage.TYPE_BYTE_GRAY)));
        assertFalse(connector.hasImage());

        connector.presentNewImage(new BufferedImage(24, 32, BufferedImage.TYPE_BYTE_GRAY), null);
        assertTrue(connector.hasImage());

        assertTrue(connector.offerBuffer(new BufferedImage(24, 32, BufferedImage.TYPE_BYTE_GRAY)));
        assertTrue(connector.hasImage());
    }

    @Test
    public void testOfferAccepts1() {
        DrawingConnector<Object> connector = create(24, 32);

        BufferedImage accepted = new BufferedImage(24, 32, BufferedImage.TYPE_BYTE_GRAY);
        callGC();
        assertTrue(connector.offerBuffer(accepted));
        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertSame(accepted, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(24, returned.getWidth());
        assertEquals(32, returned.getHeight());
    }

    @Test
    public void testOfferAccepts2() {
        DrawingConnector<Object> connector = create(24, 32);
        connector.setRequiredWidth(46, 12);

        BufferedImage accepted = new BufferedImage(46, 12, BufferedImage.TYPE_BYTE_GRAY);
        callGC();
        assertTrue(connector.offerBuffer(accepted));
        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);

        assertSame(accepted, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(46, returned.getWidth());
        assertEquals(12, returned.getHeight());
    }

    @Test
    public void testOfferRefuses1() {
        DrawingConnector<Object> connector = create(24, 32);

        BufferedImage buffer = new BufferedImage(30, 32, BufferedImage.TYPE_BYTE_GRAY);
        callGC();
        assertFalse(connector.offerBuffer(buffer));

        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertNotSame(buffer, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(24, returned.getWidth());
        assertEquals(32, returned.getHeight());
    }

    @Test
    public void testOfferRefuses2() {
        DrawingConnector<Object> connector = create(24, 32);

        BufferedImage buffer = new BufferedImage(24, 30, BufferedImage.TYPE_BYTE_GRAY);
        callGC();
        assertFalse(connector.offerBuffer(buffer));

        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertNotSame(buffer, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(24, returned.getWidth());
        assertEquals(32, returned.getHeight());
    }

    @Test
    public void testOfferRefuses3() {
        DrawingConnector<Object> connector = create(24, 32);

        BufferedImage buffer = new BufferedImage(24, 32, BufferedImage.TYPE_USHORT_GRAY);
        callGC();
        assertTrue(connector.offerBuffer(buffer));

        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertNotSame(buffer, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(24, returned.getWidth());
        assertEquals(32, returned.getHeight());
    }

    @Test
    public void testOfferRefuses4() {
        DrawingConnector<Object> connector = create(30, 32);
        connector.setRequiredWidth(24, 32);

        BufferedImage buffer = new BufferedImage(30, 32, BufferedImage.TYPE_BYTE_GRAY);
        callGC();
        assertFalse(connector.offerBuffer(buffer));

        BufferedImage returned = connector.getDrawingSurface(BufferedImage.TYPE_BYTE_GRAY);
        assertNotSame(buffer, returned);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, returned.getType());
        assertEquals(24, returned.getWidth());
        assertEquals(32, returned.getHeight());
    }

    @Test
    public void testGetDrawingSurfaceWrongWidth() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width + 1, height);
        assertTrue(connector.offerBuffer(new BufferedImage(width + 1, height, type)));

        connector.setRequiredWidth(width, height);
        BufferedImage returned = connector.getDrawingSurface(type);
        assertEquals(type, returned.getType());
        assertEquals(width, returned.getWidth());
        assertEquals(height, returned.getHeight());
    }

    @Test
    public void testGetDrawingSurfaceWrongHeight() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height + 1);
        assertTrue(connector.offerBuffer(new BufferedImage(width, height + 1, type)));

        connector.setRequiredWidth(width, height);
        BufferedImage returned = connector.getDrawingSurface(type);
        assertEquals(type, returned.getType());
        assertEquals(width, returned.getWidth());
        assertEquals(height, returned.getHeight());
    }

    @Test
    public void testGetDrawingSurfaceWrongType() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height);
        assertTrue(connector.offerBuffer(new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)));

        connector.setRequiredWidth(width, height);
        BufferedImage returned = connector.getDrawingSurface(type);
        assertEquals(type, returned.getType());
        assertEquals(width, returned.getWidth());
        assertEquals(height, returned.getHeight());
    }

    @Test
    public void testOfferNewAfterWidthChange() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width + 1, height);
        assertTrue(connector.offerBuffer(new BufferedImage(width + 1, height, type)));

        connector.setRequiredWidth(width, height);

        BufferedImage buffer = new BufferedImage(width, height, type);
        callGC();
        assertTrue(connector.offerBuffer(buffer));
        assertSame(buffer, connector.getDrawingSurface(type));
    }

    @Test
    public void testTwoOfferBuffer() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height);

        callGC();
        BufferedImage image = new BufferedImage(width, height, type);
        assertTrue(connector.offerBuffer(image));
        assertFalse(connector.offerBuffer(new BufferedImage(width, height, type)));
        assertSame(image, connector.getDrawingSurface(type));
    }

    @Test
    public void testOldPresentNewImageIsReused() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height);

        callGC();
        BufferedImage image = new BufferedImage(width, height, type);
        connector.presentNewImage(image, null);
        connector.presentNewImage(new BufferedImage(width, height, type), null);
        assertSame(image, connector.getDrawingSurface(type));
    }

    @Test
    public void testPresentNewImageDiscardsFromCache1() {
        // For less memory retation, the implementation must be eager to remove
        // not needed images.

        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width + 1, height);

        callGC();
        BufferedImage image = new BufferedImage(width + 1, height, type);
        assertTrue(connector.offerBuffer(image));

        connector.setRequiredWidth(width, height);
        connector.presentNewImage(new BufferedImage(width, height, type), null);
        assertNotSame(image, connector.getDrawingSurface(type));
    }

    @Test
    public void testPresentNewImageDiscardsFromCache2() {
        // For less memory retation, the implementation must be eager to remove
        // not needed images.

        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height + 1);

        callGC();
        BufferedImage image = new BufferedImage(width, height + 1, type);
        assertTrue(connector.offerBuffer(image));

        connector.setRequiredWidth(width, height);
        connector.presentNewImage(new BufferedImage(width, height, type), null);
        assertNotSame(image, connector.getDrawingSurface(type));
    }

    @Test
    public void testOfferNewAfterHeightChange() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = create(width, height + 1);
        assertTrue(connector.offerBuffer(new BufferedImage(width, height + 1, type)));

        connector.setRequiredWidth(width, height);

        BufferedImage buffer = new BufferedImage(width, height, type);
        callGC();
        assertTrue(connector.offerBuffer(buffer));
        assertSame(buffer, connector.getDrawingSurface(type));
    }

    @Test
    public void testOfferNull() {
        DrawingConnector<Object> connector = create(12, 23);
        assertFalse(connector.offerBuffer(null));
    }

    @Test
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public void testOfferCustom() {
        DrawingConnector<Object> connector = create(12, 23);

        ColorSpace colorSpace = mock(ColorSpace.class);
        ColorModel colorModel = new ComponentColorModel(
                colorSpace, true, true, ColorModel.TRANSLUCENT, DataBuffer.TYPE_DOUBLE);
        WritableRaster raster = colorModel.createCompatibleWritableRaster(12, 23);

        assertFalse(connector.offerBuffer(new BufferedImage(
                colorModel, raster, true, new java.util.Hashtable<>())));
    }

    private Graphics2D delegate(Graphics2D g2d) {
        return new DelegateGraphics2D(g2d);
    }

    @Test
    public void testCopyMostRecentGraphicsNoImage() {
        int width = 12;
        int height = 23;
        int type = BufferedImage.TYPE_BYTE_GRAY;

        DrawingConnector<Object> connector = spy(create(width, height));
        Graphics2D g2d = spy(delegate(new BufferedImage(width, height, type).createGraphics()));
        try {
            GraphicsCopyResult<Object> result
                    = connector.copyMostRecentGraphics(g2d, width, height);
            verify(connector, never()).scaleToGraphics(
                    any(Graphics2D.class), anyInt(), anyInt(), any(BufferedImage.class), any());
            verifyZeroInteractions(g2d);

            assertFalse(result.isPainted());
            assertNull(result.getPaintResult());
        } finally {
            g2d.dispose();
        }
    }

    @Test
    public void testCopyMostRecentGraphics() {
        for (Object paintResult: Arrays.asList(null, new Object())) {
            int width = 12;
            int height = 23;
            int type = BufferedImage.TYPE_BYTE_GRAY;

            DrawingConnector<Object> connector = spy(create(width, height));
            Graphics2D g2d = spy(delegate(new BufferedImage(width, height, type).createGraphics()));
            try {
                BufferedImage currentImage = new BufferedImage(width, height, type);
                connector.presentNewImage(currentImage, paintResult);

                GraphicsCopyResult<Object> result
                        = connector.copyMostRecentGraphics(g2d, width, height);
                verify(connector).scaleToGraphics(
                        same(g2d), eq(width), eq(height), same(currentImage), same(paintResult));
                // Actually, this does not need to be this call but a generic
                // and good check would be too difficult.
                verify(g2d).drawImage(same(currentImage), isNull(BufferedImageOp.class), eq(0), eq(0));

                assertTrue(result.isPainted());
                assertSame(paintResult, result.getPaintResult());
            } finally {
                g2d.dispose();
            }
        }
    }

    @Test
    public void testCopyMostRecentGraphicsDifferentDimImage() {
        for (Object paintResult: Arrays.asList(null, new Object())) {
            int width = 12;
            int height = 23;
            int type = BufferedImage.TYPE_BYTE_GRAY;

            DrawingConnector<Object> connector = spy(create(width, height));
            Graphics2D g2d = spy(delegate(new BufferedImage(width, height, type).createGraphics()));
            try {
                BufferedImage currentImage = new BufferedImage(width + 1, height, type);
                connector.presentNewImage(currentImage, paintResult);

                GraphicsCopyResult<Object> result
                        = connector.copyMostRecentGraphics(g2d, width, height);
                verify(connector).scaleToGraphics(
                        same(g2d), eq(width), eq(height), same(currentImage), same(paintResult));
                // Actually, this does not need to be this call but a generic
                // and good check would be too difficult.
                verify(g2d).drawImage(
                        same(currentImage), isNull(BufferedImageOp.class), eq(0), eq(0));

                assertTrue(result.isPainted());
                assertSame(paintResult, result.getPaintResult());
            } finally {
                g2d.dispose();
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructor1() {
        create(-1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructor2() {
        create(10, -1);
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidCopyMostRecentGraphics1() {
        DrawingConnector<Object> connector = create(10, 12);
        connector.copyMostRecentGraphics(null, 10, 12);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCopyMostRecentGraphics2() {
        int width = 10;
        int height = 12;
        DrawingConnector<Object> connector = create(width, height);

        Graphics2D g2d = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            connector.copyMostRecentGraphics(g2d, -1, height);
        } finally {
            g2d.dispose();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCopyMostRecentGraphics3() {
        int width = 10;
        int height = 12;
        DrawingConnector<Object> connector = create(width, height);

        Graphics2D g2d = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            connector.copyMostRecentGraphics(g2d, width, -1);
        } finally {
            g2d.dispose();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidPresentNewImage() {
        DrawingConnector<Object> connector = create(10, 12);
        connector.presentNewImage(null, new Object());
    }
}
