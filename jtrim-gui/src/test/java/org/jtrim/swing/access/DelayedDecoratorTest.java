package org.jtrim.swing.access;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
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
@SuppressWarnings("deprecation")
public class DelayedDecoratorTest {
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

    private static DelayedDecorator create1(
            DecoratorPanelFactory mainDecorator,
            long decoratorPatience,
            TimeUnit timeUnit) {
        return new DelayedDecorator(mainDecorator, decoratorPatience, timeUnit);
    }

    private static DelayedDecorator create2(
            DecoratorPanelFactory immediateDecorator,
            DecoratorPanelFactory mainDecorator,
            long decoratorPatience,
            TimeUnit timeUnit) {
        return new DelayedDecorator(immediateDecorator, mainDecorator, decoratorPatience, timeUnit);
    }

    @Test
    public void testProperties1() {
        DecoratorPanelFactory mainDecorator = mock(DecoratorPanelFactory.class);
        for (TimeUnit unit: TimeUnit.values()) {
            for (long time: Arrays.asList(0L, 1L, 5L)) {
                DelayedDecorator decorator = create1(mainDecorator, time, unit);
                assertSame(mainDecorator, decorator.getMainDecorator());
                assertTrue(decorator.getImmediateDecorator() == InvisiblePanelFactory.INSTANCE);
                assertEquals(time, decorator.getDecoratorPatience(unit));
            }
        }

        for (TimeUnit unit: TimeUnit.values()) {
            DelayedDecorator decorator = create1(mainDecorator, Long.MAX_VALUE, unit);
            assertSame(mainDecorator, decorator.getMainDecorator());
            assertTrue(decorator.getImmediateDecorator() == InvisiblePanelFactory.INSTANCE);
            assertTrue(decorator.getDecoratorPatience(unit)
                    >= unit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
        }
    }

    @Test
    public void testProperties2() {
        DecoratorPanelFactory immediateDecorator = mock(DecoratorPanelFactory.class);
        DecoratorPanelFactory mainDecorator = mock(DecoratorPanelFactory.class);
        for (TimeUnit unit: TimeUnit.values()) {
            for (long time: Arrays.asList(0L, 1L, 5L)) {
                DelayedDecorator decorator = create2(immediateDecorator, mainDecorator, time, unit);
                assertSame(mainDecorator, decorator.getMainDecorator());
                assertSame(immediateDecorator, decorator.getImmediateDecorator());
                assertEquals(time, decorator.getDecoratorPatience(unit));
            }
        }

        for (TimeUnit unit: TimeUnit.values()) {
            DelayedDecorator decorator = create2(immediateDecorator, mainDecorator, Long.MAX_VALUE, unit);
            assertSame(mainDecorator, decorator.getMainDecorator());
            assertSame(immediateDecorator, decorator.getImmediateDecorator());
            assertTrue(decorator.getDecoratorPatience(unit)
                    >= unit.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create1(null, 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor2() {
        create1(mock(DecoratorPanelFactory.class), -1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor3() {
        create1(mock(DecoratorPanelFactory.class), 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor4() {
        create2(null, mock(DecoratorPanelFactory.class), 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor5() {
        create2(mock(DecoratorPanelFactory.class), null, 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructor6() {
        create2(mock(DecoratorPanelFactory.class), mock(DecoratorPanelFactory.class), -1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor7() {
        create2(mock(DecoratorPanelFactory.class), mock(DecoratorPanelFactory.class), 1, null);
    }
}
