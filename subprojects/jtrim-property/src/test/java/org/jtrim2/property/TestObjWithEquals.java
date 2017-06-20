package org.jtrim2.property;

import java.util.Objects;

final class TestObjWithEquals {
    public static final TestObjWithEquals EMPTY = new TestObjWithEquals("");

    public final String str;

    public TestObjWithEquals(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.str);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        if (obj == this) return true;

        final TestObjWithEquals other = (TestObjWithEquals) obj;
        return Objects.equals(this.str, other.str);
    }
}
