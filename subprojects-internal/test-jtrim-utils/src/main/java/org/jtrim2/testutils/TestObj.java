package org.jtrim2.testutils;

import java.util.Objects;

public class TestObj {
    private final Object value;

    public TestObj(Object value) {
        this.value = value;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TestObj other = (TestObj) obj;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + value + '}';
    }
}
