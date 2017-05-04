package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.collections.EqualityComparator;

/**
 *
 * @author Kelemen Attila
 */
final class TestObjWithIdentity {
    public static final EqualityComparator<TestObjWithIdentity> STR_CMP = StrCmp.INSTANCE;
    public static final TestObjWithIdentity EMPTY = new TestObjWithIdentity("");

    public final String str;

    public TestObjWithIdentity(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }

    private enum StrCmp implements EqualityComparator<TestObjWithIdentity> {
        INSTANCE;

        @Override
        public boolean equals(TestObjWithIdentity obj1, TestObjWithIdentity obj2) {
            if (obj1 == null) return obj2 == null;
            if (obj2 == null) return false;

            return Objects.equals(obj1.str, obj2.str);
        }
    }
}
