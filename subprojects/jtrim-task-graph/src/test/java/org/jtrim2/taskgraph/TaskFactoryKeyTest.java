package org.jtrim2.taskgraph;

import org.jtrim2.testutils.TestObj;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskFactoryKeyTest {
    @Test
    public void testNullCustomKey() {
        TaskFactoryKey<?, ?> key = new TaskFactoryKey<>(TestOutput.class, TestArg.class);

        assertSame(TestOutput.class, key.getResultType());
        assertSame(TestArg.class, key.getFactoryArgType());
        assertNull(key.getKey());

        String str = key.toString();
        assertNotNull(str);
        assertTrue(str.contains(TestOutput.class.getSimpleName()));
        assertTrue(str.contains(TestArg.class.getSimpleName()));
    }

    @Test
    public void testNonNullCustomKey() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        assertSame(TestOutput.class, key.getResultType());
        assertSame(TestArg.class, key.getFactoryArgType());
        assertSame(customKey, key.getKey());

        String str = key.toString();
        assertNotNull(str);
        assertTrue(str.contains(TestOutput.class.getSimpleName()));
        assertTrue(str.contains(TestArg.class.getSimpleName()));
        assertTrue(str.contains(customKey.toString()));
    }

    @Test
    public void testEquals() {
        CustomKey customKey1 = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key1 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey1);

        CustomKey customKey2 = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key2 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey2);

        assertTrue(key1.equals(key2));
        assertTrue(key2.equals(key1));
        assertEquals("hash", key1.hashCode(), key2.hashCode());
    }

    private static Object nullObject() {
        return null;
    }

    @Test
    public void testCompareWithNull() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<TestOutput, TestArg> key = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);
        assertFalse(key.equals(nullObject()));
    }

    @Test
    public void testCompareWithWrongType() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        Object otherObj = "My-Test-Key-1";
        assertFalse(key.equals(otherObj));
    }

    @Test
    public void testCompareSame() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);
        assertTrue(key.equals(key));
    }

    @Test
    public void testCompareDifferentCustomKey() {
        CustomKey customKey1 = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key1 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey1);

        String customKey2 = "My-Test-Key-2";
        TaskFactoryKey<?, ?> key2 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey2);

        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void testCompareDifferentOutputType() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key1 = new TaskFactoryKey<>(Integer.class, TestArg.class, customKey);
        TaskFactoryKey<?, ?> key2 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void testCompareDifferentArgType() {
        CustomKey customKey = new CustomKey("My-Test-Key-1");
        TaskFactoryKey<?, ?> key1 = new TaskFactoryKey<>(TestOutput.class, String.class, customKey);
        TaskFactoryKey<?, ?> key2 = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void testWithKeyFactory() {
        Object oldCustomKey = "OldCustomKey-testWithKeyFactory";
        Object newCustomKey = "NewCustomKey-testWithKeyFactory";

        TaskFactoryKey<TestOutput, TestArg> src
                = new TaskFactoryKey<>(TestOutput.class, TestArg.class, oldCustomKey);

        TaskFactoryKey<TestOutput, TestArg> expected
                = new TaskFactoryKey<>(TestOutput.class, TestArg.class, newCustomKey);

        TaskFactoryKey<TestOutput, TestArg> newKey = src.withKey(newCustomKey);
        assertEquals(expected, newKey);
    }

    @Test
    public void testWithFactoryArgType() {
        Object customKey = "TestCustomKey-testWithFactoryArgType";

        TaskFactoryKey<TestOutput, TestArg> src
                = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        TaskFactoryKey<TestOutput, TestArg2> expected
                = new TaskFactoryKey<>(TestOutput.class, TestArg2.class, customKey);

        TaskFactoryKey<TestOutput, TestArg2> newKey = src.withFactoryArgType(TestArg2.class);

        assertEquals(expected, newKey);
    }

    @Test
    public void testWithResultType() {
        Object customKey = "TestCustomKey-testWithResultType";

        TaskFactoryKey<TestOutput, TestArg> src
                = new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);

        TaskFactoryKey<TestOutput2, TestArg> expected
                = new TaskFactoryKey<>(TestOutput2.class, TestArg.class, customKey);

        TaskFactoryKey<TestOutput2, TestArg> newKey = src.withResultType(TestOutput2.class);

        assertEquals(expected, newKey);
    }

    private static final class CustomKey extends TestObj {
        public CustomKey(Object str) {
            super(str);
        }
    }

    private static final class TestOutput extends TestObj {
        public TestOutput(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestOutput2 extends TestObj {
        public TestOutput2(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestArg extends TestObj {
        public TestArg(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestArg2 extends TestObj {
        public TestArg2(Object strValue) {
            super(strValue);
        }
    }
}
