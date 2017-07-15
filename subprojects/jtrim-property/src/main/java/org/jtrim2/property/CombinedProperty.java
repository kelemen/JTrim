package org.jtrim2.property;

import java.util.Objects;
import java.util.function.BiFunction;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;

final class CombinedProperty<R> implements PropertySource<R> {
    private final PropertySource<?> src1;
    private final PropertySource<?> src2;
    private final CombinedValues<?, ?, ? extends R> valueRef;

    public <T, U> CombinedProperty(
            PropertySource<? extends T> src1,
            PropertySource<? extends U> src2,
            BiFunction<? super T, ? super U, ? extends R> valueCombiner) {
        this.src1 = src1;
        this.src2 = src2;
        this.valueRef = new CombinedValues<>(src1, src2, valueCombiner);
    }

    @Override
    public R getValue() {
        return valueRef.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return ListenerRefs.combineListenerRefs(
                src1.addChangeListener(listener),
                src2.addChangeListener(listener));
    }

    private static final class CombinedValues<T, U, R> {
        private final PropertySource<? extends T> src1;
        private final PropertySource<? extends U> src2;
        private final BiFunction<? super T, ? super U, ? extends R> valueCombiner;

        public CombinedValues(
                PropertySource<? extends T> src1,
                PropertySource<? extends U> src2,
                BiFunction<? super T, ? super U, ? extends R> valueCombiner) {
            this.src1 = Objects.requireNonNull(src1, "src1");
            this.src2 = Objects.requireNonNull(src2, "src2");
            this.valueCombiner = Objects.requireNonNull(valueCombiner, "valueCombiner");
        }

        public R getValue() {
            T value1 = src1.getValue();
            U value2 = src2.getValue();
            return valueCombiner.apply(value1, value2);
        }
    }
}
