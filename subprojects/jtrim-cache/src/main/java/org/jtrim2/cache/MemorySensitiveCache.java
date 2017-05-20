package org.jtrim2.cache;

import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.collections.ElementRefIterable;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.collections.RefList.ElementRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 * A cache implementation which limits the maximum cumulative size of the
 * objects stored. There is also a user specified limit on the maximum number of
 * objects to be cached.
 * <P>
 * This implementation favors object implementing the {@link MemoryHeavyObject}
 * interface. The size of such objects are requested through
 * {@link MemoryHeavyObject#getApproxMemorySize()} method call. Other objects
 * (not implementing {@code MemoryHeavyObject}) assumed to have a size of
 * 128 bytes. Note that the minimum size of an object is always assumed to be
 * at least 128 bytes overriding the size returned by the object to be cached.
 * The size of the object is not expected to change while the object is in the
 * cache and is retrieved only once while the object is in the cache.
 * <P>
 * {@code MemorySensitiveCache} works by holding hard references to cached
 * objects, so soft and weak references will not be garbage collected
 * as long as these objects are in the cache. Note that this cache will not
 * maintain hard references to objects when a {@link ReferenceType#HardRefType}
 * is requested because such references will still reachable as long as there is
 * hard reference to the returned {@link VolatileReference volatile reference}
 * and the cache cannot efficiently check if those volatile references were
 * garbage collected.
 * <P>
 * When the cache cannot hold anymore references (either because there too many
 * references or their cumulative size is too large) it will remove the object
 * earliest referenced from the cache. The object can be referenced either by
 * creating a new reference to it or by retrieving it though a returned
 * volatile reference.
 * <P>
 * When users are reasoning about the memory consumption of their code, they
 * should assume that this cache holds to the maximum number of allowed
 * references and those references are useless.
 *
 * <h3>Thread safety</h3>
 * This class is completely thread-safe and its methods can be called from any
 * thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>, so they can
 * be called in any context (e.g.: while holding a lock).
 */
public final class MemorySensitiveCache implements ObjectCache {
    private static final long MINIMUM_MEMSIZE = 128;
    private static final int DEFAULT_MAX_OBJECT_COUNT = 1024;

    private final long maximumCacheSize;
    private final int maximumObjectsToCache;

    private final ReentrantLock mainLock;

    private boolean consistent;

    private long cachedObjectsSize;
    private final IdentityHashMap<Object, ElementRef<CachedObjectDescriptor>> cachedObjects;
    private final RefList<CachedObjectDescriptor> cachedList;

    /**
     * Initializes an empty cache with the given maximum cumulative size of
     * cached objects. The maximum number of cached objects is 1024.
     *
     * @param maximumCacheSize the maximum cumulative size of cached
     *   objects. This argument must be larger than zero.
     *
     * @throws IllegalArgumentException thrown if the argument is zero or
     *   a negative integer
     */
    public MemorySensitiveCache(long maximumCacheSize) {
        this(maximumCacheSize, DEFAULT_MAX_OBJECT_COUNT);
    }

    /**
     * Initializes an empty cache with the given maximum number and maximum
     * cumulative size of cached objects.
     *
     * @param maximumCacheSize the maximum cumulative size of cached
     *   objects. This argument must be larger than zero.
     * @param maximumObjectsToCache the maximum number of objects to cache.
     *   This argument must be larger than zero.
     *
     * @throws IllegalArgumentException thrown if any of the arguments is zero
     *   or a negative integer
     */
    public MemorySensitiveCache(long maximumCacheSize,
            int maximumObjectsToCache) {

        ExceptionHelper.checkArgumentInRange(maximumObjectsToCache,
                1, Integer.MAX_VALUE, "maximumObjectsToCache");
        ExceptionHelper.checkArgumentInRange(maximumCacheSize,
                1, Long.MAX_VALUE, "maximumCacheSize");

        this.mainLock = new ReentrantLock();
        // We need +1 because the new object is inserted before old objects
        // are removed.
        this.cachedObjects = new IdentityHashMap<>(maximumObjectsToCache + 1);
        this.cachedList = new RefLinkedList<>();

        this.cachedObjectsSize = 0;
        this.maximumCacheSize = maximumCacheSize;
        this.maximumObjectsToCache = maximumObjectsToCache;
        this.consistent = true;
    }

    /**
     * Removes every cached reference from this cache. The already returned
     * volatile references may still remain valid if they are referenced
     * as a soft or weak reference. Note however that any returned volatile
     * reference with the {@link ReferenceType#UserRefType} will return
     * {@code null} after this method returns.
     */
    public void clearCache() {
        Iterable<ElementRef<CachedObjectDescriptor>> cacheRefs;
        cacheRefs = new ElementRefIterable<>(cachedList);

        mainLock.lock();
        try {
            for (ElementRef<CachedObjectDescriptor> cacheRef: cacheRefs) {
                cacheRef.setElement(null);
            }

            cachedList.clear();
            cachedObjects.clear();
            cachedObjectsSize = 0;
            consistent = true;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the cumulative size of the currently cached objects.
     * This method never returns a negative integer and always returns a value
     * lower than {@link #getMaximumCacheSize() getMaximumCacheSize()}.
     *
     * @return the cumulative size of the currently cached objects
     */
    public long getCurrentSize() {
        long result;

        mainLock.lock();
        try {
            repairConsistency();
            result = cachedObjectsSize;
        } finally {
            mainLock.unlock();
        }

        return result;
    }

    /**
     * Returns the maximum allowed cumulative size of the cached objects.
     * The result of this method is the same as it was specified at construction
     * time and never changes during the lifetime of this cache.
     *
     * @return the maximum allowed cumulative size of the cached objects
     */
    public long getMaximumCacheSize() {
        return maximumCacheSize;
    }

    /**
     * Returns the maximum number of cached objects allowed in this cache
     * concurrently.
     * The result of this method is the same as it was specified at construction
     * time and never changes during the lifetime of this cache.
     *
     * @return the maximum number of cached objects allowed in this cache
     *   concurrently
     */
    public int getMaximumObjectsToCache() {
        return maximumObjectsToCache;
    }

    /**
     * {@inheritDoc }
     * <h3>Additional information</h3>
     * This cache will not maintain references to objects if they were requested
     * as {@link ReferenceType#NoRefType} or {@link ReferenceType#HardRefType}.
     * In case one of these two types is specified an unmaintained reference
     * will be returned (as returned by
     * {@link GenericReference#createReference(java.lang.Object, org.jtrim2.cache.ReferenceType) GenericReference.createReference(Object, ReferenceType)}).
     */
    @Override
    public <V> VolatileReference<V> getReference(V obj, ReferenceType refType) {
        if (obj == null || refType == ReferenceType.NoRefType) {
            return NoVolatileReference.getInstance();
        }
        else if (refType == ReferenceType.HardRefType) {
            // We do not store hard references in this cache
            // since we might prevent it from garbage collected
            // even if noone else references it.
            // Avoiding this would require a more complex and less efficient
            // code with little gain.
            return GenericReference.createHardReference(obj);
        }
        else {
            CachedObjectDescriptor cachedDescr
                    = new CachedObjectDescriptor(obj, getObjectSize(obj));

            if (cachedDescr.isTooLarge()) {
                // We cannot store larger than maximumCacheSize
                // objects in this cache, so just return simple reference.
                return GenericReference.createReference(obj, refType);
            }

            AtomicLong useCount;
            ElementRef<CachedObjectDescriptor> newRef;
            mainLock.lock();
            try {
                newRef = addToCachedObjects(cachedDescr);
                useCount = cachedDescr.getUseCount();
                useCount.incrementAndGet();
            } finally {
                mainLock.unlock();
            }

            return new ObjectRef<>(obj, refType, newRef, useCount);
        }
    }

    /**
     * Returns the string representation of this cache in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "MemorySensitiveCache{"
                + getCurrentSize() + "/" + maximumCacheSize + '}';
    }

    private void repairConsistency() {
        assert mainLock.isHeldByCurrentThread();

        if (!consistent) {
            clearCache();
        }
    }

    private boolean isCacheOverflowed() {
        return (cachedObjectsSize > maximumCacheSize
                || cachedObjects.size() > maximumObjectsToCache);
    }

    private void checkCacheSize() {
        assert mainLock.isHeldByCurrentThread();

        repairConsistency();

        consistent = false;

        ElementRef<CachedObjectDescriptor> currentRef;
        currentRef = cachedList.getFirstReference();

        while (isCacheOverflowed() && !cachedObjects.isEmpty()
                && currentRef != null) {

            ElementRef<CachedObjectDescriptor> nextRef;
            nextRef = currentRef.getNext(1);

            removeReference(currentRef);

            currentRef = nextRef;
        }

        consistent = true;
    }

    private ElementRef<CachedObjectDescriptor> appendToCacheList(
            CachedObjectDescriptor descr) {

        assert mainLock.isHeldByCurrentThread();

        ElementRef<CachedObjectDescriptor> currentRef;
        currentRef = cachedList.getLastReference();

        if (currentRef == null) {
            return cachedList.addGetReference(0, descr);
        }
        else {
            return currentRef.addAfter(descr);
        }
    }

    private ElementRef<CachedObjectDescriptor> addToCachedObjects(
            CachedObjectDescriptor cachedDescriptor) {

        assert cachedDescriptor != null;
        assert mainLock.isHeldByCurrentThread();
        assert !cachedDescriptor.isTooLarge();

        ElementRef<CachedObjectDescriptor> currentRef;

        consistent = false;

        currentRef = appendToCacheList(cachedDescriptor);
        ElementRef<CachedObjectDescriptor> oldValue = cachedObjects.put(
                cachedDescriptor.getCachedObject(),
                currentRef);

        if (oldValue != null) {
            CachedObjectDescriptor oldDescr = oldValue.getElement();
            oldValue.setElement(null);
            oldValue.remove();

            cachedObjectsSize -= oldDescr.getMemSize();
        }

        cachedObjectsSize += cachedDescriptor.getMemSize();

        consistent = true;

        checkCacheSize();

        return currentRef;
    }

    private ElementRef<CachedObjectDescriptor> createCachedObject(
            Object obj, long size) {

        assert mainLock.isHeldByCurrentThread();

        CachedObjectDescriptor result = new CachedObjectDescriptor(obj, size);
        return addToCachedObjects(result);
    }

    private ElementRef<CachedObjectDescriptor> referenceObject(
            Object obj, long size) {

        assert mainLock.isHeldByCurrentThread();
        assert obj != null;

        ElementRef<CachedObjectDescriptor> result;

        ElementRef<CachedObjectDescriptor> cachedDescrRef
                = cachedObjects.get(obj);

        CachedObjectDescriptor cachedDescriptor = cachedDescrRef != null
                ? cachedDescrRef.getElement()
                : null;

        if (cachedDescriptor == null) {
            result = createCachedObject(obj, size);
        }
        else {
            if (cachedDescrRef.getNext(1) != null) {
                // Remove the element and reinsert into the beginning of
                // the list.

                consistent = false;

                cachedObjects.remove(cachedDescriptor.getCachedObject());
                cachedDescrRef.setElement(null);
                cachedDescrRef.remove();
                cachedObjectsSize -= cachedDescriptor.getMemSize();

                consistent = true;

                result = addToCachedObjects(cachedDescriptor);
            }
            else {
                result = cachedDescrRef;
            }
        }

        return result;
    }

    private void removeReference(
            ElementRef<CachedObjectDescriptor> elementRef) {

        assert mainLock.isHeldByCurrentThread();

        if (elementRef != null && !elementRef.isRemoved()) {
            CachedObjectDescriptor descr = elementRef.getElement();

            elementRef.setElement(null);
            elementRef.remove();

            cachedObjects.remove(descr.getCachedObject());
            cachedObjectsSize -= descr.getMemSize();
        }
    }

    private static long getObjectSize(Object obj) {
        if (obj instanceof MemoryHeavyObject) {
            long guessedSize = ((MemoryHeavyObject)obj).getApproxMemorySize();

            return guessedSize > MINIMUM_MEMSIZE
                    ? guessedSize
                    : MINIMUM_MEMSIZE;
        }
        else {
            return MINIMUM_MEMSIZE;
        }
    }

    private class ObjectRef<T> implements VolatileReference<T> {
        private final ReferenceType refType;
        private VolatileReference<T> referent;
        private ElementRef<CachedObjectDescriptor> elementRef;
        private final ReentrantLock refLock;
        private final AtomicReference<AtomicLong> useCountRef;

        public ObjectRef(T referent, ReferenceType refType,
                ElementRef<CachedObjectDescriptor> elementRef,
                AtomicLong useCount) {

            assert referent == null
                    || referent == elementRef.getElement().getCachedObject();

            this.refType = refType;
            this.referent = GenericReference.createReference(referent, refType);
            this.elementRef = elementRef;
            this.refLock = new ReentrantLock();
            this.useCountRef = new AtomicReference<>(useCount);
        }

        // The referred object will never change (except it may become null)
        // once set in the constructor therefore it will retain its type.
        @SuppressWarnings("unchecked")
        private T getCachedObject() {
            assert mainLock.isHeldByCurrentThread()
                    && refLock.isHeldByCurrentThread();

            return (T)elementRef.getElement().getCachedObject();
        }

        @Override
        public T get() {
            T result;

            refLock.lock();
            try {
                if (referent == null) {
                    return null;
                }

                result = referent.get();
            } finally {
                refLock.unlock();
            }

            if (result == null && refType != ReferenceType.UserRefType) {
                // Don't bother trying to resurrect other kind of references
                // because it should not be possible. See below for explanation.
                return null;
            }

            refLock.lock();
            try {
                if (result == null) {
                    // If the reference is in the list, we will
                    // resurrect the reference.
                    // Notice that we can only resurrect UserRefType
                    // references since the referent did not have a hard
                    // reference (weak and soft references will not be released
                    // unless there are no hard references to the referent;
                    // and if there are no references, the cache cannot contain
                    // it either).
                    mainLock.lock();
                    try {
                        if (elementRef != null && !elementRef.isRemoved()) {
                            result = getCachedObject();
                        }
                    } finally {
                        mainLock.unlock();
                    }

                    if (result != null) {
                        long size = getObjectSize(result);
                        mainLock.lock();
                        try {
                            elementRef = createCachedObject(result, size);
                        } finally {
                            mainLock.unlock();
                        }
                    }
                    // else the element disappeared from both the cache and the
                    // reference, there is nothing to do but return null.

                }
                else {
                    long size = getObjectSize(result);
                    mainLock.lock();
                    try {
                        elementRef = referenceObject(result, size);
                    } finally {
                        mainLock.unlock();
                    }
                }
            } finally {
                refLock.unlock();
            }

            return result;
        }

        @Override
        public void clear() {
            AtomicLong useCount = useCountRef.getAndSet(null);

            refLock.lock();
            try {
                mainLock.lock();
                try {
                    repairConsistency();

                    if (useCount != null) {
                        consistent = false;
                        if (useCount.decrementAndGet() == 0) {
                            removeReference(elementRef);
                        }
                        consistent = true;
                    }
                } finally {
                    mainLock.unlock();
                }

                referent = null;
            } finally {
                refLock.unlock();
            }
        }

        @Override
        public String toString() {
            Object r = null;
            refLock.lock();
            try {
                if (referent != null) {
                    r = referent.get();
                }
            } finally {
                refLock.unlock();
            }

            return "MemorySensitiveRef{" + r + '}';
        }
    }

    private class CachedObjectDescriptor {
        private final Object cachedObject;
        private final long memSize;

        // The only reason it is not simply long
        // because we need a mutable long reference.
        // We do not need the thread-safe property
        // of this class.
        private final AtomicLong useCount; // protected by mainLock

        public CachedObjectDescriptor(Object cachedObject, long memSize) {
            this.cachedObject = cachedObject;
            this.memSize = memSize;
            this.useCount = new AtomicLong(0);
        }

        public AtomicLong getUseCount() {
            return useCount;
        }

        public Object getCachedObject() {
            return cachedObject;
        }

        public long getMemSize() {
            return memSize;
        }

        public boolean isTooLarge() {
            return memSize > maximumCacheSize;
        }
    }
}
