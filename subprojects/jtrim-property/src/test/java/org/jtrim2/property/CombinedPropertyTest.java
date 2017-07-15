package org.jtrim2.property;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.testutils.JTrimTests;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CombinedPropertyTest
extends
        JTrimTests<CombinedPropertyTest.PropertyCombiner<CombinedPropertyTest.CombinedObj>> {

    public CombinedPropertyTest() {
        super(Arrays.asList(
                CombinedProperty::new,
                PropertyFactory::combine
        ));
    }

    @Test
    public void testGetOneValue() throws Exception {
        testAll(factory -> {
            PropertySource<CombinedObj> combined = factory.combine(
                    PropertyFactory.constSource("Value1"),
                    PropertyFactory.constSource("Value2"),
                    CombinedObj::new);
            CombinedObj value = combined.getValue();
            assertEquals(new CombinedObj("Value1", "Value2"), value);
        });
    }

    @Test
    public void testGetChangedFirstValue() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            assertEquals(new CombinedObj("Value1", "Value2"), combined.getValue());

            src1.setValue("Value1-mod");

            assertEquals(new CombinedObj("Value1-mod", "Value2"), combined.getValue());
        });
    }

    @Test
    public void testGetChangedSecondValue() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            assertEquals(new CombinedObj("Value1", "Value2"), combined.getValue());

            src2.setValue("Value2-mod");

            assertEquals(new CombinedObj("Value1", "Value2-mod"), combined.getValue());
        });
    }

    @Test
    public void testNotifyChangeForFirstValue() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            Runnable listener = mock(Runnable.class);
            combined.addChangeListener(listener);

            verifyZeroInteractions(listener);
            src1.setValue("Value1-mod");
            verify(listener).run();
        });
    }

    @Test
    public void testNotifyChangeForSecondValue() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            Runnable listener = mock(Runnable.class);
            combined.addChangeListener(listener);

            verifyZeroInteractions(listener);
            src2.setValue("Value2-mod");
            verify(listener).run();
        });
    }

    @Test
    public void testUnregisterChangeListenerForFirst() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            Runnable listener = mock(Runnable.class);

            ListenerRef listenerRef = combined.addChangeListener(listener);
            listenerRef.unregister();

            src1.setValue("Value1-mod");
            verifyZeroInteractions(listener);
        });
    }

    @Test
    public void testUnregisterChangeListenerForSecond() throws Exception {
        testAll(factory -> {
            MutableProperty<String> src1 = PropertyFactory.memProperty("Value1");
            MutableProperty<String> src2 = PropertyFactory.memProperty("Value2");

            PropertySource<CombinedObj> combined
                    = factory.combine(src1, src2, CombinedObj::new);

            Runnable listener = mock(Runnable.class);

            ListenerRef listenerRef = combined.addChangeListener(listener);
            listenerRef.unregister();

            src2.setValue("Value2-mod");
            verifyZeroInteractions(listener);
        });
    }

    public interface PropertyCombiner<R> {
        public <T, U> PropertySource<R> combine(
                PropertySource<? extends T> src1,
                PropertySource<? extends U> src2,
                BiFunction<? super T, ? super U, ? extends R> valueCombiner);
    }

    public static final class CombinedObj {
        private final Object obj1;
        private final Object obj2;

        public CombinedObj(Object obj1, Object obj2) {
            this.obj1 = obj1;
            this.obj2 = obj2;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + Objects.hashCode(obj1);
            hash = 89 * hash + Objects.hashCode(obj2);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final CombinedObj other = (CombinedObj) obj;
            return Objects.equals(this.obj1, other.obj1)
                    && Objects.equals(this.obj2, other.obj2);
        }

        @Override
        public String toString() {
            return "CombinedObj{" + "obj1=" + obj1 + ", obj2=" + obj2 + '}';
        }
    }
}
