package org.jtrim.cache;

/**
 * @see GenericReference#createHardReference(java.lang.Object)
 *
 * @author Kelemen Attila
 */
final class HardVolatileReference<ReferentType> implements VolatileReference<ReferentType> {
    private volatile ReferentType referent;

    public HardVolatileReference(ReferentType referent) {
        this.referent = referent;
    }

    @Override
    public ReferentType get() {
        return referent;
    }

    @Override
    public void clear() {
        referent = null;
    }

    @Override
    public String toString() {
        return "HardReference{" + "referent=" + referent + '}';
    }
}
