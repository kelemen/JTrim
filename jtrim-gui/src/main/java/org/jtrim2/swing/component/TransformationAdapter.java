package org.jtrim2.swing.component;

import java.util.Set;
import org.jtrim2.image.transform.ZoomToFitOption;

/**
 * A convenient base class for {@link TransformationListener} implementations.
 * The methods implemented by this class does nothing by default,
 * implementations must override the method they need.
 *
 * @author Kelemen Attila
 */
public abstract class TransformationAdapter implements TransformationListener {
    /**
     * {@inheritDoc }
     */
    @Override
    public void zoomChanged() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void offsetChanged() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void flipChanged() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void rotateChanged() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void enterZoomToFitMode(Set<ZoomToFitOption> options) {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void leaveZoomToFitMode() {
    }
}
