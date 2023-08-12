package org.jtrim2.stream;

import java.util.Objects;

public final class Wrapper {
    private final String wrapped;

    public Wrapper(String wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Wrapper wrapper = (Wrapper) o;
        return Objects.equals(wrapped, wrapper.wrapped);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapped);
    }

    @Override
    public String toString() {
        return "Wrapper{" + wrapped + '}';
    }
}
