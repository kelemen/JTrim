package org.jtrim2.build;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import java.util.function.Supplier;
import org.codehaus.groovy.runtime.MethodClosure;

public final class GroovyUtils {
    public static <T, R> R configClosure(T arg, @DelegatesTo(genericTypeIndex = 0) Closure<? extends R> config) {
        @SuppressWarnings("unchecked")
        Closure<? extends R> configCopy = (Closure<? extends R>) config.clone();

        configCopy.setResolveStrategy(Closure.DELEGATE_FIRST);
        configCopy.setDelegate(arg);
        return configCopy.call(arg);
    }

    public static <T> Closure<T> toSupplierClosure(Supplier<? extends T> supplier) {
        @SuppressWarnings("unchecked")
        Closure<T> result = (Closure<T>) new MethodClosure(supplier, "get");
        return result;
    }

    private GroovyUtils() {
        throw new AssertionError();
    }
}
