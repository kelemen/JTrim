package org.jtrim2.cache;

/**
 * Defines a reference to an object which can disappear any time beyond
 * the control of the client.
 * <P>
 * This reference is similar to the references in Java (such as the
 * {@link java.lang.ref.SoftReference soft reference}) but cannot have a
 * reference queue.
 * <P>
 * The intended use of this class is for caching an object which retains
 * a considerable amount of memory and so the reference to the stored object can
 * disappear when there is not enough memory. Clients therefore must always be
 * aware that the stored object might be unavailable through this reference
 * and have a plan to fetch it again in this case.
 * <P>
 * Note that although the referenced object may disappear any time, it is
 * permitted to reappear at a later time (although in most implementations
 * it will not happen). Despite this users of a volatile reference
 * should discard the volatile reference after detecting that the referenced
 * object disappeared and should not hope for the referenced object to reappear.
 * <P>
 * The reference can also be cleared in which case the referenced object must
 * not be reachable through the volatile reference anymore (and not allowed
 * to reappear).
 * <P>
 * Clients are recommended to clear references they no longer use because
 * this allows underlying caches to discard unneeded reference.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be completely thread-safe
 * and the methods can be called from any thread.
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @param <ReferentType> the type of the stored object
 *
 * @see GenericReference
 * @see ObjectCache
 */
public interface VolatileReference<ReferentType> {
    /**
     * Returns the referenced object or {@code null} in case it disappeared.
     * This method must return the same object (in terms of "==") over the
     * lifetime of this volatile reference or {@code null}. Note however
     * that even if this method returns {@code null}, it is allowed to return
     * the original reference at a later point (though in most implementation
     * this will not occur).
     *
     * @return the referenced object or {@code null} in case it disappeared
     */
    public ReferentType get();

    /**
     * Removes the referenced object from this volatile reference. After this
     * method call {@link #get() get()} must return {@code null} forever and
     * the referenced object is not allowed to reappear.
     * <P>
     * Clients are recommended to call this method if they know that this
     * volatile reference will not be used anymore. Calling this method
     * may help the underlying cache to remove unneeded references to the
     * referenced object.
     */
    public void clear();
}
