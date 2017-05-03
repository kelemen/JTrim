package org.jtrim2.cache;

/**
 * The type of references to be used by {@link ObjectCache} implementations
 * for {@link VolatileReference volatile references} when the referenced object
 * is not in the cache. See
 * {@link GenericReference#createReference(java.lang.Object, org.jtrim2.cache.ReferenceType) GenericReference.createReference(Object, ReferenceType)}
 * for further information.
 *
 * @see GenericReference
 * @see ObjectCache
 * @author Kelemen Attila
 */
public enum ReferenceType {
    /**
     * Defines that a reference must be a hard reference
     * (a simple object reference).
     */
    HardRefType,

    /**
     * Defines that a reference must be a
     * {@link java.lang.ref.SoftReference soft reference}.
     */
    SoftRefType,

    /**
     * Defines that a reference must be a
     * {@link java.lang.ref.WeakReference weak reference}.
     */
    WeakRefType,

    /**
     * Defines that no reference must be maintained directly. The referenced
     * object should be loaded directly from the underlying cache if possible.
     */
    UserRefType,

    /**
     * Defines that the object must not be referenced at all.
     */
    NoRefType
}
