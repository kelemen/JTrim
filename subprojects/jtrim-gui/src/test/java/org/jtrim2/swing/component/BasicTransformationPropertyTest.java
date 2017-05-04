package org.jtrim2.swing.component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
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
        PropertySource<Double> offsetX = view.offsetX();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = offsetX.addChangeListener(listener);

        model.setOffset(3.0, model.getOffsetY());
        verify(listener).run();
        assertEquals(model.getOffsetX(), offsetX.getValue(), 0.0);

        model.setOffset(model.getOffsetX(), 4.0);
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

    @Test
    public void testGetOffsetXMutate() {
        MutableProperty<Double> offsetX = view.offsetX();

        Runnable listener = mock(Runnable.class);
        offsetX.addChangeListener(listener);

        offsetX.setValue(3.0);
        verify(listener).run();

        assertEquals(3.0, model.getOffsetX(), 0.0);
        assertEquals(3.0, offsetX.getValue(), 0.0);
    }

    /**
     * Test of getOffsetY method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetOffsetY() {
        PropertySource<Double> offsetY = view.offsetY();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = offsetY.addChangeListener(listener);

        model.setOffset(model.getOffsetX(), 3.0);
        verify(listener).run();
        assertEquals(model.getOffsetY(), offsetY.getValue(), 0.0);

        model.setOffset(4.0, model.getOffsetY());
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

    @Test
    public void testGetOffsetYMutate() {
        MutableProperty<Double> offsetY = view.offsetY();

        Runnable listener = mock(Runnable.class);
        offsetY.addChangeListener(listener);

        offsetY.setValue(3.0);
        verify(listener).run();

        assertEquals(3.0, model.getOffsetY(), 0.0);
        assertEquals(3.0, offsetY.getValue(), 0.0);
    }

    /**
     * Test of getZoomX method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomX() {
        PropertySource<Double> zoomX = view.zoomX();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = zoomX.addChangeListener(listener);

        model.setZoomX(3.0);
        verify(listener).run();
        assertEquals(model.getZoomX(), zoomX.getValue(), 0.0);

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        model.setZoomY(9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setZoomX(9.0);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetZoomXMutate() {
        MutableProperty<Double> zoomX = view.zoomX();

        Runnable listener = mock(Runnable.class);
        zoomX.addChangeListener(listener);

        zoomX.setValue(3.0);
        verify(listener).run();

        assertEquals(3.0, model.getZoomX(), 0.0);
        assertEquals(3.0, zoomX.getValue(), 0.0);
    }

    /**
     * Test of getZoomY method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomY() {
        PropertySource<Double> zoomY = view.zoomY();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = zoomY.addChangeListener(listener);

        model.setZoomY(3.0);
        verify(listener).run();
        assertEquals(model.getZoomY(), zoomY.getValue(), 0.0);

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.flipVertical();
        model.setRotateInRadians(9.0);
        model.setZoomX(9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.setZoomY(9.0);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetZoomYMutate() {
        MutableProperty<Double> zoomY = view.zoomY();

        Runnable listener = mock(Runnable.class);
        zoomY.addChangeListener(listener);

        zoomY.setValue(3.0);
        verify(listener).run();

        assertEquals(3.0, model.getZoomY(), 0.0);
        assertEquals(3.0, zoomY.getValue(), 0.0);
    }

    /**
     * Test of getRotateInRadians method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetRotateInRadians() {
        PropertySource<Double> rotateRad = view.rotateInRadians();

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

    @Test
    public void testGetRotateInRadiansMutate() {
        MutableProperty<Double> rotateInRad = view.rotateInRadians();

        Runnable listener = mock(Runnable.class);
        rotateInRad.addChangeListener(listener);

        rotateInRad.setValue(3.0);
        verify(listener).run();

        assertEquals(3.0, model.getRotateInRadians(), 0.0);
        assertEquals(3.0, rotateInRad.getValue(), 0.0);
    }

    /**
     * Test of getRotateInDegrees method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetRotateInDegrees() {
        PropertySource<Integer> rotateDeg = view.rotateInDegrees();

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

    @Test
    public void testGetRotateInDegreesMutate() {
        MutableProperty<Integer> rotateInDeg = view.rotateInDegrees();

        Runnable listener = mock(Runnable.class);
        rotateInDeg.addChangeListener(listener);

        rotateInDeg.setValue(3);
        verify(listener).run();

        assertEquals(3, model.getRotateInDegrees());
        assertEquals(3, rotateInDeg.getValue().intValue());
    }

    /**
     * Test of getFlipHorizontal method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetFlipHorizontal() {
        PropertySource<Boolean> flipHorizontal = view.flipHorizontal();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = flipHorizontal.addChangeListener(listener);

        model.flipHorizontal();
        verify(listener).run();
        assertEquals(model.isFlipHorizontal(), flipHorizontal.getValue());

        model.setOffset(9.0, 9.0);
        model.flipVertical();
        model.setRotateInRadians(6.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.flipHorizontal();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetFlipHorizontalMutate() {
        MutableProperty<Boolean> flipH = view.flipHorizontal();

        Runnable listener = mock(Runnable.class);
        flipH.addChangeListener(listener);

        flipH.setValue(true);
        verify(listener).run();

        assertEquals(true, model.isFlipHorizontal());
        assertEquals(true, flipH.getValue());
    }

    /**
     * Test of getFlipVertical method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetFlipVertical() {
        PropertySource<Boolean> flipVertical = view.flipVertical();

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = flipVertical.addChangeListener(listener);

        model.flipVertical();
        verify(listener).run();
        assertEquals(model.isFlipVertical(), flipVertical.getValue());

        model.setOffset(9.0, 9.0);
        model.flipHorizontal();
        model.setRotateInRadians(6.0);
        model.setZoom(9.0, 9.0);
        model.setZoomToFit(false, false);
        model.clearZoomToFit();

        listenerRef.unregister();
        model.flipVertical();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetFlipVerticalMutate() {
        MutableProperty<Boolean> flipV = view.flipVertical();

        Runnable listener = mock(Runnable.class);
        flipV.addChangeListener(listener);

        flipV.setValue(true);
        verify(listener).run();

        assertEquals(true, model.isFlipVertical());
        assertEquals(true, flipV.getValue());
    }


    /**
     * Test of getZoomToFit method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetZoomToFit() {
        PropertySource<Set<ZoomToFitOption>> zoomToFit = view.zoomToFit();

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

    @Test
    public void testGetZoomToFitMutate() {
        Set<ZoomToFitOption> value1 = EnumSet.noneOf(ZoomToFitOption.class);
        Set<ZoomToFitOption> value2 = EnumSet.of(
                ZoomToFitOption.FIT_HEIGHT, ZoomToFitOption.KEEP_ASPECT_RATIO);
        Set<ZoomToFitOption> value3 = EnumSet.allOf(ZoomToFitOption.class);
        for (Set<ZoomToFitOption> expected: Arrays.asList(value1, value2, value3, null)) {
            MutableProperty<Set<ZoomToFitOption>> zoomToFit = view.zoomToFit();

            Runnable listener = mock(Runnable.class);
            zoomToFit.addChangeListener(listener);

            zoomToFit.setValue(expected);
            verify(listener).run();

            assertEquals(expected, model.getZoomToFitOptions());
            assertEquals(expected, zoomToFit.getValue());
        }
    }

    /**
     * Test of getTransformations method, of class BasicTransformationProperty.
     */
    @Test
    public void testGetTransformations() {
        PropertySource<BasicImageTransformations> transformation = view.transformations();

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

    @Test
    public void testGetTransformationsMutate() {
        MutableProperty<BasicImageTransformations> transformations = view.transformations();

        Runnable listener = mock(Runnable.class);
        transformations.addChangeListener(listener);

        BasicImageTransformations expected = BasicImageTransformations.newRotateTransformation(3.0);
        transformations.setValue(expected);
        verify(listener).run();

        assertEquals(expected, model.getTransformations());
        assertEquals(expected, transformations.getValue());
    }
}
