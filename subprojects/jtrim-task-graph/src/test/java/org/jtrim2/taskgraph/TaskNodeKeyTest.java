package org.jtrim2.taskgraph;

import org.jtrim2.testutils.TestObj;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskNodeKeyTest {
    private static TaskFactoryKey<Object, Object> factoryKey(String key) {
        return new TaskFactoryKey<>(Object.class, Object.class, key);
    }

    @Test
    public void testNullCustomKey() {
        TaskFactoryKey<Object, Object> factoryKey = factoryKey("F");
        TaskNodeKey<Object, Object> key = new TaskNodeKey<>(factoryKey, null);

        assertSame(factoryKey, key.getFactoryKey());
        assertNull(key.getFactoryArg());

        String str = key.toString();
        assertNotNull(str);
        assertTrue(str.contains(factoryKey.toString()));
    }

    @Test
    public void testNonNullCustomKey() {
        CustomArg customArg = new CustomArg("My-Test-Arg-1");
        TaskFactoryKey<Object, Object> factoryKey = factoryKey("F");
        TaskNodeKey<Object, Object> key = new TaskNodeKey<>(factoryKey, customArg);

        assertSame(factoryKey, key.getFactoryKey());
        assertSame(customArg, key.getFactoryArg());

        String str = key.toString();
        assertNotNull(str);
        assertTrue(str.contains(factoryKey.toString()));
        assertTrue(str.contains(customArg.toString()));
    }

    @Test
    public void testEquals() {
        CustomArg customArg1 = new CustomArg("My-Test-Arg-1");
        TaskFactoryKey<Object, Object> factoryKey = factoryKey("F");
        TaskNodeKey<Object, Object> key1 = new TaskNodeKey<>(factoryKey, customArg1);

        CustomArg customArg2 = new CustomArg("My-Test-Arg-1");
        TaskNodeKey<Object, Object> key2 = new TaskNodeKey<>(factoryKey, customArg2);

        assertTrue(key1.equals(key2));
        assertTrue(key2.equals(key1));
        assertEquals("hash", key1.hashCode(), key2.hashCode());
    }

    private static Object nullObject() {
        return null;
    }

    @Test
    public void testCompareWithNull() {
        CustomArg customArg = new CustomArg("My-Test-Arg-1");
        TaskNodeKey<Object, Object> key = new TaskNodeKey<>(factoryKey("F"), customArg);
        assertFalse(key.equals(nullObject()));
    }

    @Test
    public void testCompareWithWrongType() {
        CustomArg customArg = new CustomArg("My-Test-Arg-1");
        TaskNodeKey<Object, Object> key = new TaskNodeKey<>(factoryKey("F"), customArg);

        Object otherObj = "My-Test-Arg-1";
        assertFalse(key.equals(otherObj));
    }

    @Test
    public void testCompareSame() {
        CustomArg customArg = new CustomArg("My-Test-Arg-1");
        TaskNodeKey<Object, Object> key = new TaskNodeKey<>(factoryKey("F"), customArg);
        assertTrue(key.equals(key));
    }

    @Test
    public void testCompareDifferentCustomKey() {
        CustomArg customArg1 = new CustomArg("My-Test-Arg-1");
        TaskFactoryKey<Object, Object> factoryKey = factoryKey("F");
        TaskNodeKey<Object, Object> key1 = new TaskNodeKey<>(factoryKey, customArg1);

        CustomArg customArg2 = new CustomArg("My-Test-Arg-2");
        TaskNodeKey<Object, Object> key2 = new TaskNodeKey<>(factoryKey, customArg2);

        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void testCompareDifferentFactoryKey() {
        CustomArg customArg = new CustomArg("My-Test-Arg-1");
        TaskFactoryKey<Object, Object> factoryKey1 = factoryKey("F1");
        TaskNodeKey<Object, Object> key1 = new TaskNodeKey<>(factoryKey1, customArg);

        TaskFactoryKey<Object, Object> factoryKey2 = factoryKey("F2");
        TaskNodeKey<Object, Object> key2 = new TaskNodeKey<>(factoryKey2, customArg);

        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void testWithCustomKey() {
        Object oldCustomKey = "OldCustomKey-testWithCustomKey";
        Object newCustomKey = "NewCustomKey-testWithCustomKey";

        CustomArg factoryArg = new CustomArg("Test-Arg");

        TaskNodeKey<TestOutput, CustomArg> src = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, CustomArg.class, oldCustomKey),
                factoryArg);

        TaskNodeKey<TestOutput, CustomArg> expected = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, CustomArg.class, newCustomKey),
                factoryArg);

        TaskNodeKey<TestOutput, CustomArg> newNodeKey = src.withCustomKey(newCustomKey);
        assertEquals(expected, newNodeKey);
    }

    private static final class TestOutput extends TestObj {
        public TestOutput(Object strValue) {
            super(strValue);
        }
    }

    private static final class CustomArg extends TestObj {
        public CustomArg(Object strValue) {
            super(strValue);
        }
    }
}
