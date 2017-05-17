package org.jtrim2.testutils.executor;

public interface UnsafeMockTask {
    public void execute(boolean canceled) throws Exception;
}
