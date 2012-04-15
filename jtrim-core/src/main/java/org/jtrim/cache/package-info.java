/**
 * Contains classes for caching Java objects. The caching mechanism in
 * this package can be used for large objects only.
 * <h3>Volatile references</h3>
 * Object can be cached using
 * {@link org.jtrim.cache.VolatileReference volatile references}. These
 * references may lose their content at any time. They are similar to
 * weak and soft references in Java but do not have a reference queue and
 * can be backed by an {@link org.jtrim.cache.ObjectCache} allowing for
 * user defined logic when a reference should disappear.
 *
 * @see org.jtrim.cache.JavaRefObjectCache
 * @see org.jtrim.cache.MemorySensitiveCache
 * @see org.jtrim.cache.ObjectCache
 * @see org.jtrim.cache.VolatileReference
 */
package org.jtrim.cache;
