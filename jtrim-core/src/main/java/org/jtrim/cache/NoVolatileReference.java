package org.jtrim.cache;

/**
 * @see GenericReference#getNoReference()
 *
 * @author Kelemen Attila
 */
final class NoVolatileReference<ReferentType>
        implements VolatileReference<ReferentType> {

    private static final NoVolatileReference<?> instance
            = new NoVolatileReference<>();

    @SuppressWarnings("unchecked")
    public static <V> NoVolatileReference<V> getInstance() {
        return (NoVolatileReference<V>)instance;
    }

    @Override
    public ReferentType get() {
        return null;
    }

    @Override
    public void clear() {
    }

    @Override
    public String toString() {
        return "NoReference";
    }
}
