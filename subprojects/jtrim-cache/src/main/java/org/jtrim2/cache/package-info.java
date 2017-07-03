/**
 * Contains classes for caching Java objects. The caching mechanism in
 * this package can be used for large objects only.
 * <h3>Volatile references</h3>
 * Object can be cached using
 * {@link org.jtrim2.cache.VolatileReference volatile references}. These
 * references may lose their content at any time. They are similar to
 * weak and soft references in Java but do not have a reference queue and
 * can be backed by an {@link org.jtrim2.cache.ObjectCache} allowing for
 * user defined logic when a reference should disappear.
 *
 * @see org.jtrim2.cache.ObjectCache#javaRefCache
 * @see org.jtrim2.cache.MemorySensitiveCache
 * @see org.jtrim2.cache.ObjectCache
 * @see org.jtrim2.cache.VolatileReference
 */
package org.jtrim2.cache;
