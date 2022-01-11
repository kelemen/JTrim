package org.jtrim2.build

import java.util.function.Supplier

class GroovyUtils {
    static <T> void configClosure(T arg, @DelegatesTo(genericTypeIndex = 0) Closure<?> config) {
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.delegate = arg
        config.call(arg)
    }

    static <T> Closure<T> toSupplierClosure(Supplier<? extends T> supplier) {
        return { supplier.get() }
    }

    private GroovyUtils() {
        throw new AssertionError()
    }
}

