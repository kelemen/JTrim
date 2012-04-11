package org.jtrim.concurrent.async;

/**
 * Defines a data with an index which indicates how accurate the data is. The
 * higher the value of the {@link #getIndex() index}, the more accurate the data
 * is.
 * <P>
 * This class is intended to be used with {@link AsyncDataLink} instances when
 * it is hard forward data in the order of their accuracy but the accuracy can
 * be quantified with an index. This way wrapping listeners using the
 * {@link AsyncDatas#makeSafeOrderedListener(AsyncDataListener)} method can
 * guarantee that a less accurate data will not be forwarded to the actual,
 * generic listener after a more accurate data.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class cannot be directly modified, only
 * its {@link #getRawData() data} if it is mutable. In case this data
 * object is immutable (and it is recommended to be so), then the
 * {@code OrderedData} instance is completely immutable.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the actual {@link #getRawData() data}
 *
 * @see AsyncDatas#makeSafeOrderedListener(AsyncDataListener)
 */
public final class OrderedData<DataType> {
    private final long index;
    private final DataType rawData;

    /**
     * Initializes this {@code OrderedData} with the given
     * {@link #getIndex() index} and {@link #getRawData() data}.
     *
     * @param index the index determining how accurate the data is. This
     *   argument can be any possible {@code long} value.
     * @param rawData the actual {@link #getRawData() data} whose accuracy is
     *   described by the specified index. This argument is allowed to be
     *   {@code null}.
     */
    public OrderedData(long index, DataType rawData) {
        this.index = index;
        this.rawData = rawData;
    }

    /**
     * Returns the index defining how accurate the associated
     * {@link #getRawData() data} is. Higher index means more accurate data.
     *
     * @return the index defining how accurate the associated
     *   {@link #getRawData() data} is. This method returns the value specified
     *   at construction time and can be any possible {@code long} value.
     */
    public long getIndex() {
        return index;
    }

    /**
     * Returns the actual data whose accuracy is described by the
     * {@link #getIndex() index}. Higher index means more accurate data.
     *
     * @return the actual data whose accuracy is described by the
     *   {@link #getIndex() index}. This method returns the value specified
     *   at construction time and may return {@code null}.
     */
    public DataType getRawData() {
        return rawData;
    }

    /**
     * Returns the string representation of this {@code OrderedData} in no
     * particular format. The string representation contains both the
     * {@link #getIndex() index} and the {@link #getRawData() data}.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "OrderedData{" + "index=" + index + ", Data=" + rawData + '}';
    }
}
