package org.jtrim2.cache;

/**
 * An {@link ObjectCache} implementation which is directly backed by the JVM.
 * This class can be used to provide volatile references created by
 * {@link GenericReference#createReference(java.lang.Object, org.jtrim2.cache.ReferenceType) GenericReference.createReference(Object, ReferenceType)}
 * through the {@code ObjectCache} interface.
 * <P>
 * This implementation of {@code ObjectCache} is intended to be used where
 * an {@code ObjectCache} is required but there is non available.
 * <P>
 * Note that this class is a singleton and its one and only instance can be
 * accessed by {@link #INSTANCE JavaRefObjectCache.INSTANCE}.
 *
 * <h3>Thread safety</h3>
 * This class is completely thread-safe and its methods can be called from any
 * thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>, so they can
 * be called in any context (e.g.: while holding a lock).
 *
 * @author Kelemen Attila
 */
public enum JavaRefObjectCache implements ObjectCache {
    /**
     * The one and only instance of this cache object.
     */
    INSTANCE;

    /**
     * Creates a volatile reference of the specified object which is directly
     * backed by the JVM. The result of this method is equivalent to:
     * <P>
     * {@code GenericReference.createReference(obj, refType)}
     *
     * @param <V> the type of the object to be referenced
     * @param obj the object to referenced. The returned volatile reference
     *   will return this object or {@code null}. This argument can be
     *   {@code null}.
     * @param refType the required reference type. This argument cannot be
     *   {@code null}.
     * @return the volatile reference of the passed object. This method
     *   will never return {@code null}.
     *
     * @throws NullPointerException thrown if the reference type is {@code null}
     */
    @Override
    public <V> VolatileReference<V> getReference(V obj, ReferenceType refType) {
        return GenericReference.createReference(obj, refType);
    }
}
