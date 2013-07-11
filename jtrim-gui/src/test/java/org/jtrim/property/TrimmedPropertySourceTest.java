package org.jtrim.property;

import org.jtrim.event.ListenerRef;
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
public class TrimmedPropertySourceTest {
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

    private static TrimmedPropertySource createWithConst(String value) {
        return new TrimmedPropertySource(PropertyFactory.constSource(value));
    }

    @Test
    public void testGetValueEmptyString() {
        TrimmedPropertySource property = createWithConst("");
        assertEquals("", property.getValue());
    }

    @Test
    public void testGetValueNull() {
        TrimmedPropertySource property = createWithConst(null);
        assertNull(property.getValue());
    }

    @Test
    public void testGetValueOnlyWhiteSpace() {
        TrimmedPropertySource property = createWithConst("   ");
        assertEquals("", property.getValue());
    }

    @Test
    public void testGetValueOnlyWhiteSpaceMiddle() {
        TrimmedPropertySource property = createWithConst("TEST str");
        assertEquals("TEST str", property.getValue());
    }


    @Test
    public void testGetValueOnlyWhiteSpaceAtStart() {
        TrimmedPropertySource property = createWithConst(" TEST-str");
        assertEquals("TEST-str", property.getValue());
    }

    @Test
    public void testGetValueOnlyWhiteSpaceAtEnd() {
        TrimmedPropertySource property = createWithConst("TEST-str ");
        assertEquals("TEST-str", property.getValue());
    }

    @Test
    public void testGetValueOnlyWhiteSpaceAround() {
        TrimmedPropertySource property = createWithConst(" TEST-str ");
        assertEquals("TEST-str", property.getValue());
    }

    @Test
    public void testListeners() {
        MutableProperty<String> wrapped = PropertyFactory.memProperty(null, true);
        TrimmedPropertySource property = new TrimmedPropertySource(wrapped);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);
        verifyZeroInteractions(listener);

        wrapped.setValue("");
        verify(listener).run();

        wrapped.setValue(" TEST-STR ");
        verify(listener, times(2)).run();

        wrapped.setValue(null);
        verify(listener, times(3)).run();

        listenerRef.unregister();
        wrapped.setValue("TEST-STR");

        verifyNoMoreInteractions(listener);
    }
}
