package org.jtrim2.swing.component;

import java.util.Collections;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.junit.Test;

public class TransformationAdapterTest {
    /**
     * Not much to test but that the methods does not throw exceptions.
     */
    @Test
    public void testMethods() {
        TransformationAdapterImpl listener = new TransformationAdapterImpl();

        listener.enterZoomToFitMode(Collections.<ZoomToFitOption>emptySet());
        listener.flipChanged();
        listener.leaveZoomToFitMode();
        listener.offsetChanged();
        listener.rotateChanged();
        listener.zoomChanged();
    }

    public class TransformationAdapterImpl extends TransformationAdapter {
    }
}
