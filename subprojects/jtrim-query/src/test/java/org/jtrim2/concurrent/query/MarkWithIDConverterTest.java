package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class MarkWithIDConverterTest {
    /**
     * Test of convertData method, of class MarkWithIDConverter.
     */
    @Test
    public void testConvertData() {
        MarkWithIDConverter<Object> converter = new MarkWithIDConverter<>();

        Object data1 = new TestObject("DATA1");
        Object data2 = new TestObject("DATA1");

        DataWithUid<Object> converted1 = converter.convertData(data1);
        DataWithUid<Object> converted2 = converter.convertData(data2);

        assertSame(data1, converted1.getData());
        assertSame(data2, converted2.getData());

        assertNotSame(data1, converted1.getID());
        assertNotSame(data2, converted1.getID());

        assertNotSame(data1, converted2.getID());
        assertNotSame(data2, converted2.getID());

        assertNotSame(converted1.getID(), converted2.getID());
    }

    /**
     * Test of toString method, of class MarkWithIDConverter.
     */
    @Test
    public void testToString() {
        assertNotNull(new MarkWithIDConverter<>().toString());
    }

    private static class TestObject {
        private final String str;

        public TestObject(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
