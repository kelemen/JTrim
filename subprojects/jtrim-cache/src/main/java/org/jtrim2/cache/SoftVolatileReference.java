package org.jtrim2.cache;

import java.lang.ref.SoftReference;

/**
 * @see GenericReference#createSoftReference(java.lang.Object)
 */
final class SoftVolatileReference<ReferentType>
        implements VolatileReference<ReferentType> {

    private final SoftReference<ReferentType> referent;

    public SoftVolatileReference(ReferentType referent) {
        this.referent = new SoftReference<>(referent);
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
        return "SoftReference{" + referent.get() + '}';
    }
}
