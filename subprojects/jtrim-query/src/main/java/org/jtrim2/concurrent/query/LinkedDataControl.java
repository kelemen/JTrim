package org.jtrim2.concurrent.query;

import java.util.Objects;

/**
 * Defines a {@link AsyncDataController#controlData(Object) control object} for
 * {@link AsyncDataController} instances controlling the data retrieval process
 * of two {@link AsyncDataLink} instances. {@code AsyncDataController}
 * implementations understanding this class will then send the
 * {@link #getMainControlData() main control data} to the {@code AsyncDataLink}
 * they consider the primary {@code AsyncDataLink} instance and the
 * {@link #getSecondaryControlData() secondary control data} to the
 * {@code AsyncDataLink} instance they consider secondary.
 * <P>
 * Note that the {@code AsyncDataLink} instance created by the
 * {@link AsyncLinks#convertResultAsync(AsyncDataLink, AsyncDataQuery)} method
 * understands this class.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class cannot be directly modified, only
 * its internal control objects if they are mutable. In case these control
 * objects are immutable (and they are recommended to be so), then the
 * {@code LinkedDataControl} instance is completely immutable.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see AsyncLinks#convertResultAsync(AsyncDataLink, AsyncDataQuery)
 * @see AsyncQueries#convertResultsAsync(AsyncDataQuery, AsyncDataQuery)
 */
public final class LinkedDataControl {
    private final Object mainControlData;
    private final Object secondaryControlData;

    /**
     * Creates and initializes the {@code LinkedDataControl} instance with the
     * specified control objects.
     *
     * @param mainControlData the control object used to control the primary
     *   {@code AsyncDataLink} instance. This argument cannot be {@code null}.
     * @param secondaryControlData the control object used to control the
     *   secondary {@code AsyncDataLink} instance. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public LinkedDataControl(Object mainControlData, Object secondaryControlData) {
        Objects.requireNonNull(mainControlData, "mainControlData");
        Objects.requireNonNull(secondaryControlData, "secondaryControlData");

        this.mainControlData = mainControlData;
        this.secondaryControlData = secondaryControlData;
    }

    /**
     * Returns the control object used to control the primary
     * {@code AsyncDataLink} instance.
     *
     * @return the control object used to control the primary
     *   {@code AsyncDataLink} instance. This method never returns {@code null}.
     */
    public Object getMainControlData() {
        return mainControlData;
    }

    /**
     * Returns the control object used to control the secondary
     * {@code AsyncDataLink} instance.
     *
     * @return the control object used to control the secondary
     *   {@code AsyncDataLink} instance. This method never returns {@code null}.
     */
    public Object getSecondaryControlData() {
        return secondaryControlData;
    }

    /**
     * Returns the string representation of this {@code LinkedDataControl} in no
     * particular format. The string representation contains the string
     * representation of both control objects.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "LinkedDataControl{"
                + "MainControlData=" + mainControlData
                + ", SecondaryControlData=" + secondaryControlData + '}';
    }
}
