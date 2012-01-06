package org.jtrim.cache;

import java.lang.ref.WeakReference;

/**
 * @see GenericReference#createWeakReference(java.lang.Object)
 *
 * @author Kelemen Attila
 */
final class WeakVolatileReference<ReferentType>
        implements VolatileReference<ReferentType> {

    private final WeakReference<ReferentType> referent;

    public WeakVolatileReference(ReferentType referent) {
        this.referent = new WeakReference<>(referent);
    }

    @Override
    public ReferentType get() {
        return referent.get();
    }

    @Override
    public void clear() {
        referent.clear();
    }

    @Override
    public String toString() {
        return "WeakReference{" + referent.get() + '}';
    }
}
