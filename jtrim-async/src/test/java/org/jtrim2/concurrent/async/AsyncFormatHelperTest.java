package org.jtrim2.concurrent.async;

import java.util.Arrays;
import java.util.Collections;
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
public class AsyncFormatHelperTest {
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
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AsyncFormatHelper.class);
    }

    /**
     * Test of indentText method, of class AsyncFormatHelper.
     */
    @Test
    public void testIndentText() {
        assertEquals("  null", AsyncFormatHelper.indentText(null, true));
        assertEquals("  ", AsyncFormatHelper.indentText("", true));
        assertEquals("  LINE", AsyncFormatHelper.indentText("LINE", true));
        assertEquals("  LINE1\n  LINE2", AsyncFormatHelper.indentText("LINE1\nLINE2", true));
        assertEquals("  LINE1\n  LINE2\n  LINE3",
                AsyncFormatHelper.indentText("LINE1\nLINE2\nLINE3", true));

        assertEquals("null", AsyncFormatHelper.indentText(null, false));
        assertEquals("", AsyncFormatHelper.indentText("", false));
        assertEquals("LINE", AsyncFormatHelper.indentText("LINE", false));
        assertEquals("LINE1\n  LINE2", AsyncFormatHelper.indentText("LINE1\nLINE2", false));
        assertEquals("LINE1\n  LINE2\n  LINE3",
                AsyncFormatHelper.indentText("LINE1\nLINE2\nLINE3", false));
    }

    /**
     * Test of toIndentedString method, of class AsyncFormatHelper.
     */
    @Test
    public void testToIndentedString() {
        assertEquals("  null", AsyncFormatHelper.toIndentedString(null, true));
        assertEquals("  null", AsyncFormatHelper.toIndentedString(new TestObj(null), true));
        assertEquals("  ", AsyncFormatHelper.toIndentedString(new TestObj(""), true));
        assertEquals("  LINE", AsyncFormatHelper.toIndentedString(new TestObj("LINE"), true));
        assertEquals("  LINE1\n  LINE2",
                AsyncFormatHelper.toIndentedString(new TestObj("LINE1\nLINE2"), true));
        assertEquals("  LINE1\n  LINE2\n  LINE3",
                AsyncFormatHelper.toIndentedString(new TestObj("LINE1\nLINE2\nLINE3"), true));

        assertEquals("null", AsyncFormatHelper.toIndentedString(null, false));
        assertEquals("null", AsyncFormatHelper.toIndentedString(new TestObj(null), false));
        assertEquals("", AsyncFormatHelper.toIndentedString(new TestObj(""), false));
        assertEquals("LINE", AsyncFormatHelper.toIndentedString(new TestObj("LINE"), false));
        assertEquals("LINE1\n  LINE2",
                AsyncFormatHelper.toIndentedString(new TestObj("LINE1\nLINE2"), false));
        assertEquals("LINE1\n  LINE2\n  LINE3",
                AsyncFormatHelper.toIndentedString(new TestObj("LINE1\nLINE2\nLINE3"), false));
    }

    /**
     * Test of isSingleLine method, of class AsyncFormatHelper.
     */
    @Test
    public void testIsSingleLine() {
        assertTrue(AsyncFormatHelper.isSingleLine(null));
        assertTrue(AsyncFormatHelper.isSingleLine(""));
        assertTrue(AsyncFormatHelper.isSingleLine("LINE"));
        assertTrue(AsyncFormatHelper.isSingleLine("LI\rNE"));

        assertFalse(AsyncFormatHelper.isSingleLine("\n"));
        assertFalse(AsyncFormatHelper.isSingleLine("LINE\n"));
        assertFalse(AsyncFormatHelper.isSingleLine("\nLINE"));
        assertFalse(AsyncFormatHelper.isSingleLine("LINE1\nLINE2"));
    }

    @Test
    public void testAppendIndented_Object_StringBuilderNull() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented((Object)null, result);
        assertEquals("null", result.toString());
    }

    @Test
    public void testAppendIndented_Object_StringBuilderNull2() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented(new TestObj(null), result);
        assertEquals("null", result.toString());
    }

    @Test
    public void testAppendIndented_Object_StringBuilderSingleLine() {
        StringBuilder result = new StringBuilder();
        String str = "SINGLE LINE";
        AsyncFormatHelper.appendIndented(new TestObj(str), result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testAppendIndented_Object_StringBuilderTwoLines() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented(new TestObj("LINE1\nLINE2"), result);
        assertEquals("\n  LINE1\n  LINE2", result.toString());
    }

    @Test
    public void testAppendIndented_Object_StringBuilderThreeLines() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented(new TestObj("LINE1\nLINE2\nLINE3"), result);
        assertEquals("\n  LINE1\n  LINE2\n  LINE3", result.toString());
    }

    @Test
    public void testAppendIndented_String_StringBuilderNull() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented((String)null, result);
        assertEquals("null", result.toString());
    }

    @Test
    public void testAppendIndented_String_StringBuilderSingleLine() {
        StringBuilder result = new StringBuilder();
        String str = "SINGLE LINE";
        AsyncFormatHelper.appendIndented(str, result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testAppendIndented_String_StringBuilderTwoLines() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented("LINE1\nLINE2", result);
        assertEquals("\n  LINE1\n  LINE2", result.toString());
    }

    @Test
    public void testAppendIndented_String_StringBuilderThreeLines() {
        StringBuilder result = new StringBuilder();
        AsyncFormatHelper.appendIndented("LINE1\nLINE2\nLINE3", result);
        assertEquals("\n  LINE1\n  LINE2\n  LINE3", result.toString());
    }

    /**
     * Test of arrayToString method, of class AsyncFormatHelper.
     */
    @Test
    public void testArrayToString() {
        assertEquals("[]", AsyncFormatHelper.arrayToString(new Object[0]));
        assertEquals("null", AsyncFormatHelper.arrayToString(null));
        assertEquals("[null]", AsyncFormatHelper.arrayToString(new String[]{null}));
        assertEquals("[ELEMENT1]", AsyncFormatHelper.arrayToString(new Object[]{"ELEMENT1"}));

        assertEquals("[\nELEMENT1\nELEMENT2]",
                AsyncFormatHelper.arrayToString(new Object[]{"ELEMENT1", "ELEMENT2"}));
        assertEquals("[\nELEMENT1\nELEMENT2\nELEMENT3]",
                AsyncFormatHelper.arrayToString(new Object[]{"ELEMENT1", "ELEMENT2", "ELEMENT3"}));
    }

    /**
     * Test of collectionToString method, of class AsyncFormatHelper.
     */
    @Test
    public void testCollectionToString() {
        assertEquals("[]", AsyncFormatHelper.collectionToString(Collections.emptySet()));
        assertEquals("null", AsyncFormatHelper.collectionToString(null));
        assertEquals("[null]", AsyncFormatHelper.collectionToString(Arrays.asList((String)null)));
        assertEquals("[ELEMENT1]", AsyncFormatHelper.collectionToString(Arrays.asList("ELEMENT1")));

        assertEquals("[\nELEMENT1\nELEMENT2]",
                AsyncFormatHelper.collectionToString(Arrays.asList("ELEMENT1", "ELEMENT2")));
        assertEquals("[\nELEMENT1\nELEMENT2\nELEMENT3]",
                AsyncFormatHelper.collectionToString(Arrays.asList("ELEMENT1", "ELEMENT2", "ELEMENT3")));
    }

    private static class TestObj {
        private final String strValue;

        public TestObj(String strValue) {
            this.strValue = strValue;
        }

        @Override
        public String toString() {
            return strValue;
        }
    }
}
