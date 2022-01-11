package org.jtrim2.concurrent.query;

import java.util.Objects;

/**
 * Defines an object and its ID which uniquely identifies the object within the
 * JVM. That is, two objects with the same ID (according to
 * {@link Object#equals(Object) equals} are considered to be equivalent
 * objects).
 * <P>
 * This class overrides {@link #equals(Object) equals} and
 * {@link #hashCode() hashCode}, so that two {@code DataWithUid} instances are
 * considered equivalent, if and only if, their ID equal (according to their
 * {@code equals} method).
 *
 * <h2>Thread safety</h2>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class cannot be directly modified, only
 * its properties if they are mutable. In case both the {@link #getID() ID} and
 * the {@link #getData() data} is immutable, then the {@code DataWithUid} is
 * completely immutable.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data held by the {@code DataWithUid}
 *   instance
 *
 * @see AsyncQueries#cacheByID(AsyncDataQuery, org.jtrim2.cache.ReferenceType, org.jtrim2.cache.ObjectCache, int)
 * @see AsyncLinks#markResultWithUid(AsyncDataLink)
 * @see AsyncQueries#markResultsWithUid(AsyncDataQuery)
 */
public final class DataWithUid<DataType> {
    private final DataType data;
    private final Object id;

    /**
     * Creates and initializes the {@code DataWithUid} instance with the
     * argument as the {@link #getData() data} and the {@link #getID() ID}.
     *
     * @param data the data held by the {@code DataWithUid} instance and also
     *   the ID. That is, both the {@link #getData() getData} and the
     *   {@link #getID() getID} methods will return this object. This argument
     *   is allowed to be {@code null}.
     */
    public DataWithUid(DataType data) {
        this(data, data);
    }

    /**
     * Creates and initializes the {@code DataWithUid} instance with the given
     * {@link #getData() data} and {@link #getID() ID}.
     *
     * @param data the data held by the {@code DataWithUid} instance. This
     *   argument is allowed to be {@code null}.
     * @param id the ID identifying the given data. That is, only this ID needs
     *   to be compared to determine if two data is equivalent. This argument
     *   can be {@code null} but note that {@code null} ID can only be
     *   equivalent to another {@code null} ID.
     */
    public DataWithUid(DataType data, Object id) {
        this.data = data;
        this.id = id;
    }

    /**
     * Returns the data specified at construction time. If two data is
     * considered to be equivalent should be determined by their
     * {@link #getID() ID}.
     *
     * @return the data specified at construction time. This method may return
     *   {@code null} if {@code null} was specified at construction time.
     */
    public DataType getData() {
        return data;
    }

    /**
     * Returns the ID which is used to determine if two {@link #getData() data}
     * is to be considered equivalent. That is, if the IDs are equivalent
     * according to their {@link Object#equals(Object) equals} method, the data
     * they identify should be considered equal as well. Note that it is allowed
     * for the data objects to have an {@link #equals(Object) equals} method
     * inconsistent with the IDs.
     *
     * @return the ID which is used to determine if two {@link #getData() data}
     *   is to be considered equivalent. This method may return {@code null} and
     *   {@code null} values can be equivalent only to other {@code null}
     *   values.
     */
    public Object getID() {
        return id;
    }

    /**
     * Checks whether the passed object is a {@code DataWithUid} and has an
     * equivalent ID or not. That is, this method returns {@code true}, if and
     * only if, the passed object is an instance of {@code DataWithUid} and
     * its ID is also {@code equals} to the ID of this {@code DataWithUid}.
     *
     * @param obj the object to which this {@code DataWithUid} is to be compared
     *   to. This argument can be {@code null}, in which case this method
     *   returns {@code false}.
     * @return {@code true}, the passed object is an instance of
     *   {@code DataWithUid} and its ID is also {@code equals} to the ID of this
     *   {@code DataWithUid}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataWithUid<?> other = (DataWithUid<?>) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Returns a hash code which can be used in has tables and is compatible
     * with the {@link #equals(Object) equals} method.
     *
     * @return the hash code which can be used in has tables and is compatible
     *   with the {@link #equals(Object) equals} method
     *
     * @see Object#equals(Object)
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 469 + Objects.hashCode(this.id);
    }

    /**
     * Returns the string representation of this {@code DataWithUid} in no
     * particular format. The string representation contains the string
     * representation of both the {@link #getID() ID} and the
     * {@link #getData() data}.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "ID=" + id + ", Data=" + data;
    }
}
