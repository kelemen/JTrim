package org.jtrim.taskgraph;

import java.util.Objects;
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

    private static final class CustomKey {
        private final String str;

        public CustomKey(String str) {
            this.str = str;
        }

        @Override
        public int hashCode() {
            return 295 + Objects.hashCode(str);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final CustomKey other = (CustomKey)obj;
            return Objects.equals(this.str, other.str);
        }

        @Override
        public String toString() {
            return "CustomKey{" + "str=" + str + '}';
        }
    }

    private static final class TestOutput {
    }

    private static final class TestArg {
    }
}
