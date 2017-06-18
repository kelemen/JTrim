package org.jtrim2.testutils;

import java.util.Collection;
import java.util.List;
import org.jtrim2.collections.CollectionsEx;
import org.junit.Before;

public abstract class JTrimTests<F> {
    private final List<F> factories;

    public JTrimTests(Collection<? extends F> factories) {
        this.factories = CollectionsEx.readOnlyCopy(factories);
    }

    @Before
    public void clearInterrupts() {
        Thread.interrupted();
    }

    public static int getThreadCount() {
        return Math.min(4, 2 * Runtime.getRuntime().availableProcessors());
    }

    protected final void testAll(FactoryTestMethod<F> testMethod) throws Exception {
        for (F factory: factories) {
            testMethod.doTest(factory);
        }
    }
}
