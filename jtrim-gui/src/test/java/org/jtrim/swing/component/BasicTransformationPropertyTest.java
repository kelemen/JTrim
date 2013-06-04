package org.jtrim.swing.component;

import java.util.Set;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.transform.BasicImageTransformations;
import org.jtrim.image.transform.ZoomToFitOption;
import org.jtrim.property.PropertySource;
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
public class BasicTransformationPropertyTest {
    private BasicTransformationModel model;
    private  BasicTransformationProperty view;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        model = new BasicTransformationModel();
        view = new BasicTransformationProperty(model);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getOffsetX method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetOffsetX() {
        PropertySource<Double> offsetX = view.getOffsetX();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = offsetX.addChangeListener(listener);

        model.setOffset(3.0, model.getOffsetY());
        verify(listener).run();
        assertEquals(model.getOffsetX(), offsetX.getValue(), 0.0);

        //model.setOffset(model.getOffsetX(), 4.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setOffset(9.0, model.getOffsetY());
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getOffsetY method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetOffsetY() {
        PropertySource<Double> offsetY = view.getOffsetY();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = offsetY.addChangeListener(listener);

        model.setOffset(model.getOffsetX(), 3.0);
        verify(listener).run();
        assertEquals(model.getOffsetY(), offsetY.getValue(), 0.0);

        //model.setOffset(4.0, model.getOffsetY());
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setOffset(model.getOffsetX(), 9.0);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getZoomX method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomX() {
        PropertySource<Double> zoomX = view.getZoomX();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = zoomX.addChangeListener(listener);

        model.setZoomX(3.0);
        verify(listener).run();
        assertEquals(model.getZoomX(), zoomX.getValue(), 0.0);

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        //model.setZoomY(9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setZoomX(9.0);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getZoomY method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomY() {
        PropertySource<Double> zoomY = view.getZoomY();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = zoomY.addChangeListener(listener);

        model.setZoomY(3.0);
        verify(listener).run();
        assertEquals(model.getZoomY(), zoomY.getValue(), 0.0);

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        //model.setZoomX(9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setZoomY(9.0);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getRotateInRadians method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetRotateInRadians() {
        PropertySource<Double> rotateRad = view.getRotateInRadians();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = rotateRad.addChangeListener(listener);

        model.setRotateInRadians(3.0);
        verify(listener).run();
        assertEquals(model.getRotateInRadians(), rotateRad.getValue(), 0.0);

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setRotateInRadians(6.0);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getRotateInDegrees method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetRotateInDegrees() {
        PropertySource<Integer> rotateDeg = view.getRotateInDegrees();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = rotateDeg.addChangeListener(listener);

        model.setRotateInDegrees(3);
        verify(listener).run();
        assertEquals(Integer.valueOf(model.getRotateInDegrees()), rotateDeg.getValue());

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setRotateInDegrees(9);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getFlipHorizontal method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetFlipHorizontal() {
        PropertySource<Boolean> flipHorizontal = view.getFlipHorizontal();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = flipHorizontal.addChangeListener(listener);

        model.flipHorizontal();
        verify(listener).run();
        assertEquals(model.isFlipHorizontal(), flipHorizontal.getValue());

        model.setOffset(9.0, 9.0);
        //model.flipVertical();
        model.setRotateInRadians(6.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.flipHorizontal();
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getFlipVertical method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetFlipVertical() {
        PropertySource<Boolean> flipVertical = view.getFlipVertical();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = flipVertical.addChangeListener(listener);

        model.flipVertical();
        verify(listener).run();
        assertEquals(model.isFlipVertical(), flipVertical.getValue());

        model.setOffset(9.0, 9.0);
        //model.flipHorizontal();
        model.setRotateInRadians(6.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.flipVertical();
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getZoomToFit method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomToFit() {
        PropertySource<Set<ZoomToFitOption>> zoomToFit = view.getZoomToFit();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = zoomToFit.addChangeListener(listener);

        model.setZoomToFit(false, false);
        verify(listener).run();
        assertEquals(model.getZoomToFitOptions(), zoomToFit.getValue());

        model.clearZoomToFit();
        verify(listener, times(2)).run();
        assertEquals(model.getZoomToFitOptions(), zoomToFit.getValue());

        model.setOffset(9.0, 9.0);
        model.flipVertical();
        model.flipHorizontal();
        model.setRotateInRadians(6.0);
        model.setZoom(9.0, 9.0);

        listenerRef.unregister();
        model.setZoomToFit(true, true);
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of getTransformations method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetTransformations() {
        PropertySource<BasicImageTransformations> transformation = view.getTransformations();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = transformation.addChangeListener(listener);

        model.flipVertical();
        verify(listener).run();
        assertEquals(model.getTransformations(), transformation.getValue());

        model.flipHorizontal();
        verify(listener, times(2)).run();
        assertEquals(model.getTransformations(), transformation.getValue());

        model.setOffset(3.0, 3.0);
        verify(listener, times(3)).run();
        assertEquals(model.getTransformations(), transformation.getValue());

        model.setRotateInRadians(3.0);
        verify(listener, times(4)).run();
        assertEquals(model.getTransformations(), transformation.getValue());

        model.setZoom(3.0, 3.0);
        verify(listener, times(5)).run();
        assertEquals(model.getTransformations(), transformation.getValue());

        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.flipVertical();
        verifyNoMoreInteractions(listener);
    }
}
