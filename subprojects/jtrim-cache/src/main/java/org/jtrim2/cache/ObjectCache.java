package org.jtrim2.cache;

/**
 * Defines a cache of some java objects. This cache is designed to be used
 * to cache objects retaining considerable memory. Although cache
 * implementations must allow any objects to be cached, caching objects
 * comparable to a {@link VolatileReference VolatileReference} in size
 * is useless and will only cause more memory to be used.
 * <P>
 * Every implementation of this object cache must allow any java object to
 * be cached, however they may prefer some over others (the preference is
 * implementation dependent) and can handle such objects more efficiently.
 * <P>
 * This cache returns {@code VolatileReference}s of objects and these volatile
 * references can be used to access cached datas.
 * <P>
 * The most basic implementation of this cache is {@link ObjectCache#javaRefCache()}
 * and this implementation should be used by default if there is no
 * specific cache is available.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be completely thread-safe
 * and the methods can be called from any thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @see ObjectCache#javaRefCache()
 * @see MemorySensitiveCache
 * @see VolatileReference
 */
public interface ObjectCache {
    /**
     * Returns an {@link ObjectCache} implementation which is directly backed by the JVM.
     * This class can be used to provide volatile references created by
     * {@link GenericReference#createReference(Object, ReferenceType) GenericReference.createReference}
     * through the {@code ObjectCache} interface.
     * <P>
     * This implementation of {@code ObjectCache} is intended to be used where
     * an {@code ObjectCache} is required but there is none available.
     *
     * @return an {@link ObjectCache} implementation which is directly backed by the JVM. This
     *   method never returns {@code null}.
     */
    public static ObjectCache javaRefCache() {
        return GenericReference::createReference;
    }

    /**
     * Returns a new volatile cached reference of the given object.
     * The passed object will be cached at the discretion of the
     * implementation.
     * <P>
     * If the implementation chooses the cache the
     * passed object, the volatile reference should keep returning
     * the cached object (unless the reference was cleared) as long as the
     * cached object is in the cache. In case the cached object is removed
     * from the cache for any reason, the volatile reference may keep
     * returning it (e.g.: using soft or weak references).
     * <P>
     * Note that {@link VolatileReference#clear() clearing} the reference
     * will not necessarily remove the object from this cache but
     * implementations should (but not required) do so if every returned
     * volatile reference of the passed object is cleared.
     * <P>
     * Note that in most cases the used reference type is recommended to be
     * {@link ReferenceType#WeakRefType} or {@link ReferenceType#SoftRefType},
     * so the object will effectively remain cached unless the JVM decides that
     * it needs more memory. The main reason not to use them is that these
     * references (if reachable) will cause a small performance penalty for the
     * garbage collector.
     *
     * @param <V> the type of the cached object
     * @param obj the object to be cached. The returned volatile reference will
     *   return this object or {@code null}. This argument can be {@code null}
     *   but in this case the returned reference will always return
     *   {@code null} and is effectively useless.
     * @param refType the type of the reference to be used if the passed
     *   object was removed from this cache. Implementations are expected
     *   to honor this argument but may ignore it if they document it so.
     *   This argument cannot be {@code null}.
     * @return the volatile reference of the passed object ({@code obj}). This
     *   method must never return {@code null}.
     *
     * @throws NullPointerException thrown if the reference type is {@code null}
     */
    public <V> VolatileReference<V> getReference(V obj, ReferenceType refType);
}
