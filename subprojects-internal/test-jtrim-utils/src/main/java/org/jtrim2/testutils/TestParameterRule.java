package org.jtrim2.testutils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.collections.CollectionsEx;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class TestParameterRule implements TestRule {
    private final Impl<?, ?> impl;

    public <T> TestParameterRule(
            List<T> parameterValues,
            Consumer<? super T> parameterSetter) {

        this.impl = new Impl<>(null, parameterValues, parameterSetter);
    }

    public <T, A extends Annotation> TestParameterRule(
            Class<? extends A> filterAnnotationClass,
            Function<? super A, ? extends T[]> parameterValuesProvider,
            Consumer<? super T> parameterSetter) {

        this.impl = new Impl<>(filterAnnotationClass, parameterValuesProvider, parameterSetter);
    }

    public <T> TestParameterRule(
            Class<? extends Annotation> filterAnnotationClass,
            List<T> parameterValues,
            Consumer<? super T> parameterSetter) {

        this.impl = new Impl<>(filterAnnotationClass, parameterValues, parameterSetter);
    }

    private static RetentionPolicy getRetentionPolicy(Class<? extends Annotation> annotation) {
        Retention retention = annotation.getAnnotation(Retention.class);
        if (retention == null) {
            return RetentionPolicy.CLASS;
        }
        return retention.value();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return impl.apply(base, description);
    }

    private static final class Impl<T, A extends Annotation> {
        private final Class<? extends A> filterAnnotationClass;
        private final Function<A, List<T>> parameterValuesProvider;
        private final Consumer<? super T> parameterSetter;

        public Impl(
                Class<? extends A> filterAnnotationClass,
                Function<? super A, ? extends T[]> parameterValuesProvider,
                Consumer<? super T> parameterSetter) {

            Objects.requireNonNull(filterAnnotationClass, "filterAnnotationClass");
            Objects.requireNonNull(parameterValuesProvider, "parameterValuesProvider");
            Objects.requireNonNull(parameterSetter, "parameterSetter");

            this.filterAnnotationClass = Objects.requireNonNull(filterAnnotationClass, "filterAnnotationClass");
            this.parameterValuesProvider = annotation -> {
                T[] parameterValues = parameterValuesProvider.apply(annotation);
                return ArraysEx.viewAsList(parameterValues.clone());
            };
            this.parameterSetter = Objects.requireNonNull(parameterSetter, "parameterSetter");
        }

        public Impl(
                Class<? extends A> filterAnnotationClass,
                List<T> parameterValues,
                Consumer<? super T> parameterSetter) {

            List<T> parameterValuesCopy = CollectionsEx.readOnlyCopy(parameterValues);

            this.filterAnnotationClass = filterAnnotationClass;
            this.parameterValuesProvider = annotation -> parameterValuesCopy;
            this.parameterSetter = Objects.requireNonNull(parameterSetter, "parameterSetter");

            if (filterAnnotationClass != null) {
                RetentionPolicy retentionPolicy = getRetentionPolicy(filterAnnotationClass);
                if (retentionPolicy != RetentionPolicy.RUNTIME) {
                    throw new IllegalArgumentException("Filter annotation must have runtime retention policy,"
                            + " but has: " + retentionPolicy);
                }
            }
        }

        public Statement apply(Statement base, Description description) {
            A annotation = filterAnnotationClass != null
                    ? description.getAnnotation(filterAnnotationClass)
                    : null;

            if (annotation == null && filterAnnotationClass != null) {
                return base;
            }

            List<T> parameterValues = parameterValuesProvider.apply(annotation);

            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Assume.assumeFalse("Parameter values should be set.", parameterValues.isEmpty());

                    Throwable toThrow = null;

                    for (T parameter : parameterValues) {
                        try {
                            parameterSetter.accept(parameter);
                            base.evaluate();
                        } catch (Throwable ex) {
                            StringBuilder message = new StringBuilder();
                            message.append("Test failed for parameter");
                            if (filterAnnotationClass != null) {
                                message.append(" (");
                                message.append(filterAnnotationClass.getSimpleName());
                                message.append(")");
                            }
                            message.append(": ");
                            message.append(parameter);

                            Exception paramEx = new Exception(message.toString(), ex);
                            if (toThrow == null) {
                                toThrow = paramEx;
                            } else {
                                toThrow.addSuppressed(paramEx);
                            }
                        }
                    }

                    if (toThrow != null) {
                        throw toThrow;
                    }
                }
            };
        }
    }
}
