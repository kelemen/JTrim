package org.jtrim.concurrent.async;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cache.GenericReference;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;

/**
 *
 * @author Kelemen Attila
 */
public final class ManualCache implements ObjectCache {
    private final ConcurrentMap<Object, AtomicReference<?>> cachedValues;

    public ManualCache() {
        this.cachedValues = new ConcurrentHashMap<>();
    }

    public void removeFromCache(Object value) {
        AtomicReference<?> valueRef = cachedValues.remove(value);
        if (valueRef != null) {
            valueRef.set(null);
        }
    }

    @Override
    public <V> VolatileReference<V> getReference(V obj, ReferenceType refType) {
        if (refType == ReferenceType.NoRefType) {
            return GenericReference.getNoReference();
        }
        if (refType == ReferenceType.HardRefType) {
            return GenericReference.createHardReference(obj);
        }
        AtomicReference<V> storedRef = null;
        do {
            // Safe since the atomic reference holds "obj" or null.
            @SuppressWarnings(value = "unchecked")
            AtomicReference<V> objRef = (AtomicReference<V>)cachedValues.get(obj);
            if (objRef == null) {
                objRef = new AtomicReference<>(obj);
                if (cachedValues.putIfAbsent(obj, objRef) == null) {
                    storedRef = objRef;
                }
            }
        } while (storedRef == null);
        final VolatileReference<V> javaRef = GenericReference.createReference(obj, refType);
        final AtomicReference<AtomicReference<V>> resultRef = new AtomicReference<>(storedRef);
        return new VolatileReference<V>() {
            @Override
            public V get() {
                V result = javaRef.get();
                if (result == null) {
                    AtomicReference<V> ref = resultRef.get();
                    result = ref != null ? ref.get() : null;
                }
                return result;
            }

            @Override
            public void clear() {
                resultRef.set(null);
            }
        };
    }

}
