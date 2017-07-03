package org.jtrim2.cache;

import java.util.Objects;

/**
 * Contains static methods to create volatile references backed by the JVM.
 * Note that you can also use {@link ObjectCache#javaRefCache()} for this purpose.
 * These methods are only for convenience when no {@link ObjectCache} is needed.
 *
 * @see ObjectCache#javaRefCache()
 */
public final class GenericReference {
    /**
     * This is a static helper class and cannot be instantiated.
     */
    private GenericReference() {
        throw new AssertionError();
    }

    /**
     * Returns a volatile reference of the passed object that is directly backed
     * by the JVM. The reference type must be specified for determining how the
     * passed object will be referenced.
     * <P>
     * The returned volatile reference will maintain the following Java
     * reference types for the specified {@link ReferenceType reference types}:
     * <ul>
     *  <li>{@code HardRefType}: hard reference
     *   (the commonly used simple reference), the referred object will
     *   never disappear unless the volatile reference is cleared</li>
     *  <li>{@code SoftRefType}:
     *   {@link java.lang.ref.SoftReference soft reference}</li>
     *  <li>{@code WeakRefType}:
     *   {@link java.lang.ref.WeakReference weak reference}</li>
     *  <li>{@code UserRefType}: not referenced</li>
     *  <li>{@code NoRefType}: not referenced</li>
     * </ul>
     * Note that after the returned volatile reference is cleared the object
     *   will no longer reachable in anyway from the returned volatile
     *   reference.
     *
     * @param <T> the type of the object to be referenced
     * @param referent the object to referenced. The returned volatile reference
     *   will return this object or {@code null}. This argument can be
     *   {@code null}.
     * @param refType the required reference type. This argument cannot be
     *   {@code null}.
     * @return the volatile reference of the passed object. This method
     *   will never return {@code null}.
     *
     * @throws NullPointerException thrown if the reference type is {@code null}
     */
    public static <T> VolatileReference<T> createReference(
            T referent, ReferenceType refType) {
        Objects.requireNonNull(refType, "refType");

        if (referent == null) {
            return getNoReference();
        }

        switch (refType) {
            case HardRefType:
                return createHardReference(referent);
            case SoftRefType:
                return createSoftReference(referent);
            case WeakRefType:
                return createWeakReference(referent);
            case UserRefType:
                return getNoReference();
            case NoRefType:
                return getNoReference();
            default:
                throw new AssertionError("Unknown reference type.");
        }
    }

    /**
     * Returns a volatile reference that stores a hard reference to
     * the specified object. The passed object will never disappear from
     * the returned volatile reference unless it is explicitly cleared.
     * <P>
     * This method is effectively equivalent to:
     * {@code createReference(referent, ReferenceType.HardRefType)}.
     *
     * @param <T> the type of the referred object
     * @param referent the object to referenced. The returned volatile reference
     *   will return this object or {@code null} if it was cleared. This
     *   argument can be {@code null}.
     *
     * @return the volatile reference of the specified object. This argument
     *   will never return {@code null} and the returned volatile reference
     *   will only lose its referred object if it was explicitly cleared.
     */
    public static <T> VolatileReference<T> createHardReference(T referent) {
        return new HardVolatileReference<>(referent);
    }

    /**
     * Returns a volatile reference which stores a soft reference to
     * the specified object. The passed object is expected to disappear from
     * the returned volatile reference only if the JVM decides that it needs
     * more memory. The JVM is required to clear this reference before
     * throwing an {@link OutOfMemoryError}.
     * <P>
     * This method is effectively equivalent to:
     * {@code createReference(referent, ReferenceType.SoftRefType)}.
     *
     * @param <T> the type of the referred object
     * @param referent the object to referenced. The returned volatile reference
     *   will return this object or {@code null}. This argument can be
     *   {@code null}.
     *
     * @return the volatile reference of the specified object. This argument
     *   will never return {@code null}.
     */
    public static <T> VolatileReference<T> createSoftReference(T referent) {
        return new SoftVolatileReference<>(referent);
    }

    /**
     * Returns a volatile reference which stores a weak reference to
     * the specified object. The passed object is expected to disappear from
     * the returned volatile reference if the garbage collector decides to clean
     * up references (the garbage collector will not try to keep this reference
     * if there are only weak references to the object). The JVM is required to
     * clear this reference before throwing an {@link OutOfMemoryError}.
     * <P>
     * This method is effectively equivalent to:
     * {@code createReference(referent, ReferenceType.WeakRefType)}.
     *
     * @param <T> the type of the referred object
     * @param referent the object to referenced. The returned volatile reference
     *   will return this object or {@code null}. This argument can be
     *   {@code null}.
     *
     * @return the volatile reference of the specified object. This argument
     *   will never return {@code null}.
     */
    public static <T> VolatileReference<T> createWeakReference(T referent) {
        return new WeakVolatileReference<>(referent);
    }

    /**
     * Returns a volatile reference which always returns {@code null}. This
     * method is expected to be used where an interface requires a volatile
     * reference and the actual object is not available. Note that
     * every general user must be able to handle this reference since from
     * their point of view this reference is equivalent to an already cleared
     * reference.
     * <P>
     * This method is effectively equivalent to:
     * {@code createReference(referent, ReferenceType.NoRefType)}.
     *
     * @param <T> the type of the (virtually) referred object. Note that
     *   no instance of this type need to exists.
     *
     * @return the volatile reference which always returns {@code null}. This
     *   argument will never return {@code null} and is immutable, so it can
     *   be shared safely across threads.
     */
    public static <T> VolatileReference<T> getNoReference() {
        return NoVolatileReference.getInstance();
    }
}
