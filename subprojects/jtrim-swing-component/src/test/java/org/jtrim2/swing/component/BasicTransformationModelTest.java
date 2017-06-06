package org.jtrim2.swing.component;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class BasicTransformationModelTest {
    private static final double DOUBLE_TOLERANCE = 0.000001;

    private static BasicTransformationModel create() {
        return new BasicTransformationModel();
    }

    private static void checkIdentityExcept(BasicTransformationModel model, TransfProperty... exceptions) {
        Set<TransfProperty> exceptSet = exceptions.length > 0
                ? EnumSet.copyOf(Arrays.asList(exceptions))
                : Collections.<TransfProperty>emptySet();

        BasicImageTransformations transf = model.getTransformations();
        if (!exceptSet.contains(TransfProperty.OFFSET)) {
            assertEquals(0.0, model.getOffsetX(), 0.0);
            assertEquals(0.0, model.getOffsetY(), 0.0);
            assertEquals(0.0, transf.getOffsetX(), 0.0);
            assertEquals(0.0, transf.getOffsetY(), 0.0);
        }
        if (!exceptSet.contains(TransfProperty.ROTATE)) {
            assertEquals(0.0, model.getRotateInRadians(), 0.0);
            assertEquals(0, model.getRotateInDegrees());
            assertEquals(0.0, transf.getRotateInRadians(), 0.0);
            assertEquals(0, transf.getRotateInDegrees());
        }
        if (!exceptSet.contains(TransfProperty.ZOOM_X)) {
            assertEquals(1.0, model.getZoomX(), 0.0);
            assertEquals(1.0, transf.getZoomX(), 0.0);
        }
        if (!exceptSet.contains(TransfProperty.ZOOM_Y)) {
            assertEquals(1.0, model.getZoomY(), 0.0);
            assertEquals(1.0, transf.getZoomY(), 0.0);
        }
        if (!exceptSet.contains(TransfProperty.FLIP_H)) {
            assertFalse(model.isFlipHorizontal());
            assertFalse(transf.isFlipHorizontal());
        }
        if (!exceptSet.contains(TransfProperty.FLIP_V)) {
            assertFalse(model.isFlipVertical());
            assertFalse(transf.isFlipVertical());
        }
        if (!exceptSet.contains(TransfProperty.ZOOM_TO_FIT)) {
            assertFalse(model.isInZoomToFitMode());
            assertNull(model.getZoomToFitOptions());
        }
    }

    @Test
    public void testInitialProperties() {
        BasicTransformationModel model = create();
        checkIdentityExcept(model);
    }

    @Test
    public void testOffset() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double offsetX = 8.0;
        double offsetY = 9.0;
        model.setOffset(offsetX, offsetY);
        verify(changeListener).run();
        verify(transfListener).offsetChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(offsetX, model.getOffsetX(), 0.0);
        assertEquals(offsetY, model.getOffsetY(), 0.0);
        checkIdentityExcept(model, TransfProperty.OFFSET);
    }

    @Test
    public void testRotateRad() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double rotate = Math.PI / 6.0;
        model.setRotateInRadians(rotate);
        verify(changeListener).run();
        verify(transfListener).rotateChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(rotate, model.getRotateInRadians(), DOUBLE_TOLERANCE);
        assertEquals(30, model.getRotateInDegrees());
        checkIdentityExcept(model, TransfProperty.ROTATE);
    }

    @Test
    public void testRotateDeg() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        int rotate = 30;
        model.setRotateInDegrees(rotate);
        verify(changeListener).run();
        verify(transfListener).rotateChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(Math.toRadians(rotate), model.getRotateInRadians(), DOUBLE_TOLERANCE);
        assertEquals(30, model.getRotateInDegrees());
        checkIdentityExcept(model, TransfProperty.ROTATE);
    }

    @Test
    public void testZoom() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double zoom = 3.0;
        model.setZoom(zoom);
        verify(changeListener).run();
        verify(transfListener).zoomChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(zoom, model.getZoomX(), 0.0);
        assertEquals(zoom, model.getZoomY(), 0.0);
        checkIdentityExcept(model, TransfProperty.ZOOM_X, TransfProperty.ZOOM_Y);
    }

    @Test
    public void testZoomXY() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double zoomX = 3.0;
        double zoomY = 4.0;
        model.setZoom(zoomX, zoomY);
        verify(changeListener).run();
        verify(transfListener).zoomChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(zoomX, model.getZoomX(), 0.0);
        assertEquals(zoomY, model.getZoomY(), 0.0);
        checkIdentityExcept(model, TransfProperty.ZOOM_X, TransfProperty.ZOOM_Y);
    }

    @Test
    public void testZoomX() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double zoomX = 3.0;
        model.setZoomX(zoomX);
        verify(changeListener).run();
        verify(transfListener).zoomChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(zoomX, model.getZoomX(), 0.0);
        checkIdentityExcept(model, TransfProperty.ZOOM_X);
    }

    @Test
    public void testZoomY() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        double zoomY = 3.0;
        model.setZoomY(zoomY);
        verify(changeListener).run();
        verify(transfListener).zoomChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(zoomY, model.getZoomY(), 0.0);
        checkIdentityExcept(model, TransfProperty.ZOOM_Y);
    }

    @Test
    public void testSetFlip() {
        for (boolean flipH: Arrays.asList(false, true)) {
            for (boolean flipV: Arrays.asList(false, true)) {
                if (!flipH && !flipV) continue;

                Runnable changeListener = mock(Runnable.class);
                TransformationListener transfListener = mock(TransformationListener.class);
                BasicTransformationModel model = create();
                model.addChangeListener(changeListener);
                model.addTransformationListener(transfListener);
                verifyZeroInteractions(changeListener, transfListener);

                model.setFlip(flipH, flipV);
                verify(changeListener).run();
                verify(transfListener).flipChanged();
                verifyNoMoreInteractions(transfListener);

                assertEquals(flipH, model.isFlipHorizontal());
                assertEquals(flipV, model.isFlipVertical());
                checkIdentityExcept(model, TransfProperty.FLIP_H, TransfProperty.FLIP_V);
            }
        }
    }

    @Test
    public void testSetFlipH() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        model.setFlipHorizontal(true);
        verify(changeListener).run();
        verify(transfListener).flipChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(true, model.isFlipHorizontal());
        checkIdentityExcept(model, TransfProperty.FLIP_H);
    }

    @Test
    public void testSetFlipV() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        model.setFlipVertical(true);
        verify(changeListener).run();
        verify(transfListener).flipChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(true, model.isFlipVertical());
        checkIdentityExcept(model, TransfProperty.FLIP_V);
    }

    @Test
    public void testFlipH() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        model.flipHorizontal();
        verify(changeListener).run();
        verify(transfListener).flipChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(true, model.isFlipHorizontal());
        checkIdentityExcept(model, TransfProperty.FLIP_H);
    }

    @Test
    public void testFlipV() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        model.flipVertical();
        verify(changeListener).run();
        verify(transfListener).flipChanged();
        verifyNoMoreInteractions(transfListener);

        assertEquals(true, model.isFlipVertical());
        checkIdentityExcept(model, TransfProperty.FLIP_V);
    }

    private static <T extends Enum<T>> Iterable<Set<T>> allPossible(final Class<T> type) {
        return () -> allPossibleItr(type);
    }


    private static <T extends Enum<T>> Iterator<Set<T>> allPossibleItr(final Class<T> type) {
        final T[] constants = type.getEnumConstants();
        final boolean[] position = new boolean[constants.length + 1];
        return new Iterator<Set<T>>() {
            @Override
            public boolean hasNext() {
                return !position[position.length - 1];
            }

            @Override
            public Set<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                Set<T> result = EnumSet.noneOf(type);
                for (int i = 0; i < constants.length; i++) {
                    if (position[i]) {
                        result.add(constants[i]);
                    }
                }

                int flipIndex = -1;
                do {
                    flipIndex++;
                    position[flipIndex] = !position[flipIndex];
                } while (!position[flipIndex]);
                return Collections.unmodifiableSet(result);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    public void testZoomToFit1() {
        for (Set<ZoomToFitOption> options: allPossible(ZoomToFitOption.class)) {
            Runnable changeListener = mock(Runnable.class);
            TransformationListener transfListener = mock(TransformationListener.class);
            BasicTransformationModel model = create();
            model.addChangeListener(changeListener);
            model.addTransformationListener(transfListener);
            verifyZeroInteractions(changeListener, transfListener);

            model.setZoomToFit(options);
            verify(changeListener).run();
            verify(transfListener).enterZoomToFitMode(eq(options));
            verifyNoMoreInteractions(transfListener);

            assertEquals(options, model.getZoomToFitOptions());
            assertTrue(model.isInZoomToFitMode());
            checkIdentityExcept(model, TransfProperty.ZOOM_TO_FIT);
        }
    }

    @Test
    public void testZoomToFit2() {
        for (Set<ZoomToFitOption> options: allPossible(ZoomToFitOption.class)) {
            Runnable changeListener = mock(Runnable.class);
            TransformationListener transfListener = mock(TransformationListener.class);
            BasicTransformationModel model = create();
            model.addChangeListener(changeListener);
            model.addTransformationListener(transfListener);
            verifyZeroInteractions(changeListener, transfListener);

            Set<ZoomToFitOption> actualOptions = options.isEmpty()
                    ? EnumSet.noneOf(ZoomToFitOption.class)
                    : EnumSet.copyOf(options);
            actualOptions.add(ZoomToFitOption.FIT_WIDTH);
            actualOptions.add(ZoomToFitOption.FIT_HEIGHT);

            model.setZoomToFit(
                    options.contains(ZoomToFitOption.KEEP_ASPECT_RATIO),
                    options.contains(ZoomToFitOption.MAY_MAGNIFY));
            verify(changeListener).run();
            verify(transfListener).enterZoomToFitMode(eq(actualOptions));
            verifyNoMoreInteractions(transfListener);

            assertEquals(actualOptions, model.getZoomToFitOptions());
            assertTrue(model.isInZoomToFitMode());
            checkIdentityExcept(model, TransfProperty.ZOOM_TO_FIT);
        }
    }

    @Test
    public void testZoomToFit3() {
        for (Set<ZoomToFitOption> options: allPossible(ZoomToFitOption.class)) {
            Runnable changeListener = mock(Runnable.class);
            TransformationListener transfListener = mock(TransformationListener.class);
            BasicTransformationModel model = create();
            model.addChangeListener(changeListener);
            model.addTransformationListener(transfListener);
            verifyZeroInteractions(changeListener, transfListener);

            model.setZoomToFit(
                    options.contains(ZoomToFitOption.KEEP_ASPECT_RATIO),
                    options.contains(ZoomToFitOption.MAY_MAGNIFY),
                    options.contains(ZoomToFitOption.FIT_WIDTH),
                    options.contains(ZoomToFitOption.FIT_HEIGHT));
            verify(changeListener).run();
            verify(transfListener).enterZoomToFitMode(eq(options));
            verifyNoMoreInteractions(transfListener);

            assertEquals(options, model.getZoomToFitOptions());
            assertTrue(model.isInZoomToFitMode());
            checkIdentityExcept(model, TransfProperty.ZOOM_TO_FIT);
        }
    }

    @Test
    public void testClearZoomToFit() {
        Set<ZoomToFitOption> options = EnumSet.allOf(ZoomToFitOption.class);

        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        model.setZoomToFit(options);
        verify(changeListener).run();
        verify(transfListener).enterZoomToFitMode(eq(options));
        verifyNoMoreInteractions(transfListener);

        model.clearZoomToFit();
        verify(changeListener, times(2)).run();
        verify(transfListener).leaveZoomToFitMode();
        verifyNoMoreInteractions(transfListener);

        checkIdentityExcept(model);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Set<ZoomToFitOption>> zoomToFitSetCaptor() {
        return (ArgumentCaptor<Set<ZoomToFitOption>>)(ArgumentCaptor<?>)ArgumentCaptor.forClass(Set.class);
    }

    @Test
    public void testZoomToFitChangeInListener() {
        Set<ZoomToFitOption> options1 = EnumSet.of(ZoomToFitOption.FIT_HEIGHT);
        final Set<ZoomToFitOption> options2 = EnumSet.of(ZoomToFitOption.FIT_WIDTH);

        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        final BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);

        Runnable changeZoomToFit = Tasks.runOnceTask(() -> {
            model.setZoomToFit(options2);
        });
        model.addChangeListener(changeZoomToFit);
        verifyZeroInteractions(changeListener, transfListener);

        ArgumentCaptor<Set<ZoomToFitOption>> args = zoomToFitSetCaptor();
        model.setZoomToFit(options1);
        verify(changeListener, times(2)).run();
        verify(transfListener, times(2)).enterZoomToFitMode(args.capture());
        verifyNoMoreInteractions(transfListener);

        assertEquals(options1, args.getAllValues().get(0));
        assertEquals(options2, args.getAllValues().get(1));
        assertTrue(model.isInZoomToFitMode());
        checkIdentityExcept(model, TransfProperty.ZOOM_TO_FIT);
    }

    @Test
    public void testSetTransformations() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);
        verifyZeroInteractions(changeListener, transfListener);

        BasicImageTransformations.Builder newTransf = new BasicImageTransformations.Builder();
        newTransf.setFlipHorizontal(true);
        newTransf.setFlipVertical(true);
        newTransf.setOffset(1.0, 2.0);
        newTransf.setRotateInRadians(1.3);
        newTransf.setZoomX(3.0);
        newTransf.setZoomY(4.0);
        BasicImageTransformations expected = newTransf.create();

        model.setTransformations(expected);
        verify(changeListener, times(4)).run();
        verify(transfListener).flipChanged();
        verify(transfListener).offsetChanged();
        verify(transfListener).rotateChanged();
        verify(transfListener).zoomChanged();
        verifyNoMoreInteractions(transfListener);

        BasicImageTransformations actual = model.getTransformations();
        assertEquals(expected.getOffsetX(), actual.getOffsetX(), DOUBLE_TOLERANCE);
        assertEquals(expected.getOffsetY(), actual.getOffsetY(), DOUBLE_TOLERANCE);
        assertEquals(expected.getRotateInRadians(), actual.getRotateInRadians(), DOUBLE_TOLERANCE);
        assertEquals(expected.getRotateInDegrees(), actual.getRotateInDegrees());
        assertEquals(expected.getZoomX(), actual.getZoomX(), DOUBLE_TOLERANCE);
        assertEquals(expected.getZoomY(), actual.getZoomY(), DOUBLE_TOLERANCE);
        assertEquals(expected.isFlipHorizontal(), actual.isFlipHorizontal());
        assertEquals(expected.isFlipVertical(), actual.isFlipVertical());

        assertEquals(expected.getOffsetX(), model.getOffsetX(), DOUBLE_TOLERANCE);
        assertEquals(expected.getOffsetY(), model.getOffsetY(), DOUBLE_TOLERANCE);
        assertEquals(expected.getRotateInRadians(), model.getRotateInRadians(), DOUBLE_TOLERANCE);
        assertEquals(expected.getRotateInDegrees(), model.getRotateInDegrees());
        assertEquals(expected.getZoomX(), model.getZoomX(), DOUBLE_TOLERANCE);
        assertEquals(expected.getZoomY(), model.getZoomY(), DOUBLE_TOLERANCE);
        assertEquals(expected.isFlipHorizontal(), model.isFlipHorizontal());
        assertEquals(expected.isFlipVertical(), model.isFlipVertical());
        assertFalse(model.isInZoomToFitMode());
        assertNull(model.getZoomToFitOptions());
    }

    @Test
    public void testLazyListener() {
        Runnable changeListener = mock(Runnable.class);
        TransformationListener transfListener = mock(TransformationListener.class);
        BasicTransformationModel model = create();
        model.addChangeListener(changeListener);
        model.addTransformationListener(transfListener);

        model.setFlip(false, false);
        model.setFlipHorizontal(false);
        model.setFlipVertical(false);
        model.setOffset(0.0, 0.0);
        model.setRotateInDegrees(0);
        model.setRotateInRadians(0.0);
        model.setZoom(1.0);
        model.setZoom(1.0, 1.0);
        model.setZoomX(1.0);
        model.setZoomY(1.0);
        model.clearZoomToFit();
        model.setTransformations(BasicImageTransformations.identityTransformation());
        checkIdentityExcept(model);

        verifyZeroInteractions(changeListener, transfListener);
    }

    @Test
    public void testLazyListenerWithZoomToFit() {
        for (Set<ZoomToFitOption> options: allPossible(ZoomToFitOption.class)) {
            Runnable changeListener = mock(Runnable.class);
            TransformationListener transfListener = mock(TransformationListener.class);
            BasicTransformationModel model = create();
            model.addChangeListener(changeListener);
            model.addTransformationListener(transfListener);
            verifyZeroInteractions(changeListener, transfListener);

            model.setZoomToFit(options);
            verify(changeListener).run();
            verify(transfListener).enterZoomToFitMode(eq(options));
            verifyNoMoreInteractions(transfListener);

            model.setZoomToFit(new HashSet<>(options));

            verify(changeListener).run();
            verify(transfListener).enterZoomToFitMode(eq(options));
            verifyNoMoreInteractions(transfListener);
        }
    }

    private enum TransfProperty {
        OFFSET,
        ROTATE,
        ZOOM_X,
        ZOOM_Y,
        FLIP_H,
        FLIP_V,
        ZOOM_TO_FIT
    }
}
