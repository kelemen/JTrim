package org.jtrim2.image.transform;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ZoomToFitOptionTest {
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
    public void testAutoGenerated() {
        for (ZoomToFitOption state: ZoomToFitOption.values()) {
            assertSame(state, ZoomToFitOption.valueOf(state.toString()));
        }
    }
}
