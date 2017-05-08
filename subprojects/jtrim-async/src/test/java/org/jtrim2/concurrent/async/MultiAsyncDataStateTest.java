package org.jtrim2.concurrent.async;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiAsyncDataStateTest {
    private static MultiAsyncDataState create(AsyncDataState... subStates) {
        return new MultiAsyncDataState(subStates);
    }

    @Test
    public void testNullSubState() {
        MultiAsyncDataState state = create((AsyncDataState)null);

        assertEquals(0.0, state.getProgress(), 0.0);
        assertEquals(0.0, state.getSubProgress(0), 0.0);
        assertNull(state.getSubState(0));
        assertEquals(1, state.getSubStateCount());
        assertArrayEquals(new Object[]{null}, state.getSubStateList().toArray());
        assertArrayEquals(new AsyncDataState[]{null}, state.getSubStates());
        assertNotNull(state.toString());
    }

    @Test
    public void testListConstructor() {
        for (int stateCount = 0; stateCount < 5; stateCount++) {
            AsyncDataState[] subStates = new AsyncDataState[stateCount];
            for (int i = 0; i < subStates.length; i++) {
                subStates[i] = mock(AsyncDataState.class);
            }

            assertArrayEquals(subStates, new MultiAsyncDataState(Arrays.asList(subStates)).getSubStates());
        }
    }

    /**
     * Test of getSubStates method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetSubStates() {
        for (int stateCount = 0; stateCount < 5; stateCount++) {
            AsyncDataState[] subStates = new AsyncDataState[stateCount];
            for (int i = 0; i < subStates.length; i++) {
                subStates[i] = mock(AsyncDataState.class);
            }

            assertArrayEquals(subStates, create(subStates).getSubStates());
        }
    }

    /**
     * Test of getSubStateList method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetSubStateList() {
        for (int stateCount = 0; stateCount < 5; stateCount++) {
            AsyncDataState[] subStates = new AsyncDataState[stateCount];
            for (int i = 0; i < subStates.length; i++) {
                subStates[i] = mock(AsyncDataState.class);
            }

            assertArrayEquals(subStates, create(subStates).getSubStateList()
                    .toArray(new AsyncDataState[0]));
        }
    }

    /**
     * Test of getSubStateCount method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetSubStateCount() {
        for (int stateCount = 0; stateCount < 5; stateCount++) {
            AsyncDataState[] subStates = new AsyncDataState[stateCount];
            for (int i = 0; i < subStates.length; i++) {
                subStates[i] = mock(AsyncDataState.class);
            }

            assertEquals(stateCount, create(subStates).getSubStateCount());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegaltestGetSubState1() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };
        create(states).getSubState(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegaltestGetSubState2() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };
        create(states).getSubState(3);
    }

    /**
     * Test of getSubState method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetSubState() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };

        MultiAsyncDataState multiState = create(states);
        for (int i = 0; i < states.length; i++) {
            assertSame(states[i], multiState.getSubState(i));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGetSubProgress1() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };
        create(states).getSubProgress(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGetSubProgress2() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };
        create(states).getSubProgress(3);
    }

    /**
     * Test of getSubProgress method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetSubProgress() {
        AsyncDataState[] states = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };

        MultiAsyncDataState multiState = create(states);
        for (int i = 0; i < states.length; i++) {
            assertEquals(states[i].getProgress(), multiState.getSubProgress(i), 0.0);
        }
    }

    /**
     * Test of getProgress method, of class MultiAsyncDataState.
     */
    @Test
    public void testGetProgress() {
        AsyncDataState[] states1 = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };

        AsyncDataState[] states2 = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.9)
        };

        AsyncDataState[] states3 = new AsyncDataState[]{
            new SimpleDataState("", 0.5),
            new SimpleDataState("", 0.4),
            new SimpleDataState("", 0.75)
        };

        AsyncDataState[] states4 = new AsyncDataState[]{
            new SimpleDataState("", 0.8),
            new SimpleDataState("", 0.25),
            new SimpleDataState("", 0.75)
        };

        MultiAsyncDataState baseState = create(states1);

        assertTrue(baseState.getProgress() <= create(states2).getProgress());
        assertTrue(baseState.getProgress() <= create(states3).getProgress());
        assertTrue(baseState.getProgress() <= create(states4).getProgress());
    }

    /**
     * Test of toString method, of class MultiAsyncDataState.
     */
    @Test
    public void testToString() {
        assertNotNull(create().toString());
        assertNotNull(create(mock(AsyncDataState.class)).toString());
        assertNotNull(create(mock(AsyncDataState.class), mock(AsyncDataState.class)).toString());
    }
}
