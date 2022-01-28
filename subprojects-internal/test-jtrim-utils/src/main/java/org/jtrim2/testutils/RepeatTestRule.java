package org.jtrim2.testutils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class RepeatTestRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        RepeatTest annotation = description.getAnnotation(RepeatTest.class);
        if (annotation == null) {
            return base;
        }
        int repeatCount = annotation.value();
        if (repeatCount <= 0) {
            throw new IllegalStateException("Unexpected repeat count " + repeatCount
                    + " on " + description.getDisplayName());
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (int i = repeatCount; i > 0; i--) {
                    base.evaluate();
                }
            }
        };
    }
}
