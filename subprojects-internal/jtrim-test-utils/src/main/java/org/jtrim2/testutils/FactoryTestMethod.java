package org.jtrim2.testutils;

public interface FactoryTestMethod<F> {
    public void doTest(F factory) throws Exception;
}
