package org.jtrim2.concurrent.query;

import java.util.Objects;
import org.jtrim2.cache.GenericReference;
import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cache.VolatileReference;

/**
 * Defines a data and a {@link VolatileReference} of the same data.
 * <P>
 * Instances of {@code RefCachedData} store a hard reference to the data, so
 * in case the {@code VolatileReference} is backed by a {@code WeakReference}
 * or a {@code SoftReference}, then the reference from the
 * {@code VolatileReference} will not disappear (unless
 * {@link VolatileReference#clear() cleared explicitly}).
 * <P>
 * It should always be the case that {@code getDataRef().get()} be the same
 * object as the {@link #getData() data} of the {@code RefCachedData} or
 * be {@code null} (if the reference disappeared from the
 * {@code VolatileReference}).
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Once created, the properties of this class cannot be changed.
 * Note however, that if the {@link #getData() data} is mutable then, it can
 * be changed. Also, the stored {@code VolatileReference} can be cleared.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of data referenced by the {@code RefCachedData}
 *
 * @see AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache)
 * @see AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, java.util.concurrent.TimeUnit)
 */
public final class RefCachedData<DataType> {
    private final DataType data;
    private final VolatileReference<DataType> dataRef;

    /**
     * Initializes this {@code RefCachedData} with the specified
     * {@link #getData() data} and {@link #getDataRef() reference} to the data.
     * <P>
     * The specified {@code VolatileReference} must return the same data
     * (in terms of "==") as the one specified or return {@code null} if the
     * data has disappeared from the reference. Note however that this is not
     * checked by this method.
     *
     * @param data the actual data which is referenced by the specified
     *   {@code VolatileReference}. This argument can be {@code null}.
     * @param dataRef the {@code VolatileReference} referencing the specified
     *   data. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code VolatileReference} is {@code null}
     */
    public RefCachedData(DataType data, VolatileReference<DataType> dataRef) {
        Objects.requireNonNull(dataRef, "dataRef");

        // dataRef.get() == data should hold (or dataRef.get() == null) but we
        // do not check it for the sake of efficiency.
        this.data = data;
        this.dataRef = dataRef;
    }

    /**
     * Initializes this {@code RefCachedData} with the specified
     * {@link #getData() data} and a {@code VolatileReference} to the data,
     * created by the specified cache.
     *
     * @param data the actual data to which a {@code VolatileReference} will be
     *   created using the specified cache. This argument can be {@code null}.
     * @param cache the {@code ObjectCache} to be used to create the
     *   {@code VolatileReference} to the specified data. This argument can be
     *   {@code null}, in which case {@link ObjectCache#javaRefCache()} will
     *   be used instead.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code ReferenceType} is {@code null}
     */
    public RefCachedData(DataType data, ObjectCache cache, ReferenceType refType) {
        this.data = data;
        this.dataRef = cache != null
                ? cache.getReference(data, refType)
                : GenericReference.createReference(data, refType);
    }

    /**
     * Returns the data which is referenced by the stored
     * {@code VolatileReference} returned by the
     * {@link #getDataRef() getDataRef} method.
     *
     * @return the data which is referenced by the stored
     *   {@code VolatileReference} returned by the
     *   {@link #getDataRef() getDataRef} method. This method may return
     *   {@code null} if {@code null} was specified at construction time.
     *
     * @see #getDataRef()
     */
    public DataType getData() {
        return data;
    }

    /**
     * Returns the {@code VolatileReference} referencing the stored
     * {@link #getData() data}.
     *
     * @return the {@code VolatileReference} referencing the stored
     *   {@link #getData() data}. This method never returns {@code null}.
     *
     * @see #getData()
     */
    public VolatileReference<DataType> getDataRef() {
        return dataRef;
    }

    /**
     * Returns the string representation of this {@code RefCachedData} in no
     * particular format. The string representation contains both the
     * {@link #getData() data} and the {@link #getDataRef() reference} to it.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "RefCachedData{" + "Data=" + data + ", DataRef=" + dataRef + '}';
    }
}
