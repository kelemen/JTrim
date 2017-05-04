package org.jtrim2.swing.component;

import java.util.Collections;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class TransformationAdapterTest {
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
