package org.jtrim.collections;

import java.util.Objects;

/**
 *
 * @author Kelemen Attila
 */
final class TestObject {
    private final String str;

    public TestObject(String str) {
        this.str = str;
    }

    @Override
    public int hashCode() {
        return 485 + Objects.hashCode(str);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final TestObject other = (TestObject)obj;
        return Objects.equals(this.str, other.str);
    }

    @Override
    public String toString() {
        return str;
    }
}
