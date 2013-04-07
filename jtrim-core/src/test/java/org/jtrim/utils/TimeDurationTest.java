package org.jtrim.utils;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class TimeDurationTest {
    private static final long[] TEST_LONGS = new long[]{
        Long.MAX_VALUE,
        Long.MIN_VALUE,
        0L,
        -1L,
        1L,
        345L,
        -654L,
        Integer.MAX_VALUE,
        Integer.MIN_VALUE
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

    @Test
    public void testGetDurationNoError() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(time, duration.getDuration(unit));
            }
        }
    }

    /**
     * Test of toNanos method, of class TimeDuration.
     */
    @Test
    public void testToNanos() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toNanos(time), duration.toNanos());
            }
        }
    }

    /**
     * Test of toMicros method, of class TimeDuration.
     */
    @Test
    public void testToMicros() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toMicros(time), duration.toMicros());
            }
        }
    }

    /**
     * Test of toMillis method, of class TimeDuration.
     */
    @Test
    public void testToMillis() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toMillis(time), duration.toMillis());
            }
        }
    }

    /**
     * Test of toSeconds method, of class TimeDuration.
     */
    @Test
    public void testToSeconds() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toSeconds(time), duration.toSeconds());
            }
        }
    }

    /**
     * Test of toMinutes method, of class TimeDuration.
     */
    @Test
    public void testToMinutes() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toMinutes(time), duration.toMinutes());
            }
        }
    }

    /**
     * Test of toHours method, of class TimeDuration.
     */
    @Test
    public void testToHours() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toHours(time), duration.toHours());
            }
        }
    }

    /**
     * Test of toDays method, of class TimeDuration.
     */
    @Test
    public void testToDays() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertEquals(unit.toDays(time), duration.toDays());
            }
        }
    }

    /**
     * Test of toString method, of class TimeDuration.
     */
    @Test
    public void testToString() {
        for (long time: TEST_LONGS) {
            for (TimeUnit unit: TimeUnit.values()) {
                TimeDuration duration = new TimeDuration(time, unit);
                assertNotNull(duration.toString());
            }
        }
    }
}
