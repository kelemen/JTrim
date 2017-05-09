package org.jtrim2.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.jtrim2.collections.ArraysEx;
import org.jtrim2.collections.CollectionsEx;

/**
 * Defines the access request passed to an {@link AccessManager}. The request
 * contains rights to which only read access is required and rights
 * which require exclusive access.
 * <P>
 * Every request also contains a {@link #getRequestID() request id} which is
 * intended to be used to identify the reason the right was requested. The ID is
 * not required to be unique amongst requests.
 * <P>
 * To create right request with only a single read or write access request you
 * can use to following convenience factory methods:
 * <ul>
 *  <li>{@link #getReadRequest(Object, Object) getReadRequest(IDType, RightType)}</li>
 *  <li>{@link #getWriteRequest(Object, Object) getWriteRequest(IDType, RightType)}</li>
 * </ul>
 * <P>
 * Conflicting rights are allowed in a single right request.
 *
 * <ol>
 *  <li>Two rights are said to be conflicting if their domains intersect and
 *  at least one of them is a write request.
 *  rights.</li>
 *
 *  <li>If the domain of rights intersect or not is defined by the
 *  implementation of the {@code AccessManager}.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such are thread-safe even in
 * the face of unsynchronized concurrent access. Note that although instances
 * are immutable, the rights ({@link #getReadRights() getReadRights()},
 * {@link #getWriteRights() () getWriteRights()}) and the
 * {@link #getRequestID() request id} are not necessarily immutable. Note
 * however that <B>it is strongly recommended to use immutable types for rights
 * and the request id</B>.
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <IDType> the type of the {@link #getRequestID() request id}. <B>The
 *   request ID is strongly recommended to be immutable.</B>
 * @param <RightType> the type of rights the request contains. <B>The rights
 *   are strongly recommended to be immutable.</B>
 *
 * @see AccessManager
 * @see AccessToken
 */
public final class AccessRequest<IDType, RightType> {
    private final IDType requestID;

    private final Collection<RightType> readRights;
    private final Collection<RightType> writeRights;

    /**
     * Creates an access request with a single read right request.
     * <P>
     * <ul>
     *  <li>{@link #getReadRights() getReadRights()} will return a collection
     *  with a single element specified by the {@code readRight} argument</li>
     *
     *  <li>{@link #getWriteRights() getWriteRights()} will return an empty
     *  collection</li>
     * </ul>
     *
     * @param <IDType> the type of the {@link #getRequestID() request ID}
     * @param <RightType> the type of the read right
     * @param requestID the ID identifying this request. See:
     *   {@link #getRequestID() getRequestID()}. This argument cannot be
     *   {@code null}.
     * @param readRight the single read right which is to be requested.
     *   This argument can be {@code null}; note however that not every
     *   {@code AccessManager} implementation accepts {@code null} rights.
     * @return the newly created access request representing the single
     *   specified read right. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the request id is {@code null}
     */
    public static <IDType, RightType>
            AccessRequest<IDType, RightType> getReadRequest(
            IDType requestID, RightType readRight) {

        return new AccessRequest<>(
                requestID, Collections.singleton(readRight), null);
    }

    /**
     * Creates an access request with a single write right request.
     * <P>
     * <ul>
     *  <li>{@link #getReadRights() getReadRights()} will return an empty
     *  collection</li>
     *
     *  <li>{@link #getWriteRights() getWriteRights()} will return a collection
     *  with a single element specified by the {@code writeRight} argument</li>
     * </ul>
     *
     * @param <IDType> the type of the {@link #getRequestID() request ID}
     * @param <RightType> the type of the read right
     * @param requestID the ID identifying this request. See:
     *   {@link #getRequestID() getRequestID()}. This argument cannot be
     *   {@code null}.
     * @param writeRight the single write right which is to be requested.
     *   This argument can be {@code null}; note however that not every
     *   {@code AccessManager} implementation accepts {@code null} rights.
     * @return the newly created access request representing the single
     *   specified write right. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the {@code requestID}
     *   is {@code null}
     */
    public static <IDType, RightType>
            AccessRequest<IDType, RightType> getWriteRequest(
            IDType requestID, RightType writeRight) {

        return new AccessRequest<>(
                requestID, null, Collections.singleton(writeRight));
    }

    /**
     * Creates a new access request with the specified read and write rights
     * and the {@link #getRequestID() request id}.
     *
     * @param requestID the identifier used to refer to this request. Such
     *   identifier does not need to unique amongst requests; they are intended
     *   to be used to determine if an {@link AccessToken access token}
     *   requested by this request can be canceled. This argument cannot be
     *   {@code null}.
     * @param readRights the read rights to be requested. This argument can be
     *   {@code null} which is equivalent to passing an empty collection. The
     *   passed collection will be copied and no reference will be retained to
     *   it by the newly created object.
     * @param writeRights the write rights to be requested. This argument can be
     *   {@code null} which is equivalent to passing an empty collection. The
     *   passed collection will be copied and no reference will be retained to
     *   it by the newly created object.
     *
     * @throws NullPointerException thrown if the {@code requestID}
     *   is {@code null}
     */
    public AccessRequest(IDType requestID,
            Collection<? extends RightType> readRights,
            Collection<? extends RightType> writeRights) {

        Objects.requireNonNull(requestID, "requestID");

        this.requestID = requestID;

        this.readRights = readRights != null
                ? CollectionsEx.readOnlyCopy(readRights)
                : Collections.<RightType>emptySet();

        this.writeRights = writeRights != null
                ? CollectionsEx.readOnlyCopy(writeRights)
                : Collections.<RightType>emptySet();
    }

    /**
     * Creates a new access request with the specified read and write rights
     * and the {@link #getRequestID() request id}.
     *
     * @param requestID the identifier used to refer to this request. Such
     *   identifier does not need to unique amongst requests; they are intended
     *   to be used to determine if an {@link AccessToken access token}
     *   requested by this request can be canceled. This argument cannot be
     *   {@code null}.
     * @param readRights the read rights to be requested. This argument can be
     *   {@code null} which is equivalent to passing an empty array. The
     *   passed array will be copied and no reference will be retained to
     *   it by the newly created object.
     * @param writeRights the write rights to be requested. This argument can be
     *   {@code null} which is equivalent to passing an empty array. The
     *   passed array will be copied and no reference will be retained to
     *   it by the newly created object.
     *
     * @throws NullPointerException thrown if the {@code requestID}
     *   is {@code null}
     */
    public AccessRequest(IDType requestID,
            RightType[] readRights, RightType[] writeRights) {

        Objects.requireNonNull(requestID, "requestID");

        this.requestID = requestID;

        this.readRights = readRights != null
                ? ArraysEx.viewAsList(readRights.clone())
                : Collections.<RightType>emptySet();

        this.writeRights = writeRights != null
                ? ArraysEx.viewAsList(writeRights.clone())
                : Collections.<RightType>emptySet();
    }

    /**
     * Returns the identifier used to refer to this request. This identifier
     * is intended to be used to determine if an {@link AccessToken access token}
     * requested by this request can be canceled.
     * {@link AccessManager AccessManagers} must pass this identifier to the
     * newly created {@code AccessToken}.
     * <P>
     * Note that this identifier is not required to unique.
     *
     * @return the identifier of this request. This method never returns
     *   {@code null}.
     */
    public IDType getRequestID() {
        return requestID;
    }

    /**
     * Returns a read-only collection of read right requests. The returned
     * rights are allowed to conflict with
     * {@link #getWriteRights() write right requests} and can (though not
     * recommended) contain the same rights multiple times.
     *
     * @return the collection of read right requests. This method never
     *   returns {@code null} but may return an empty collection.
     */
    public Collection<RightType> getReadRights() {
        return readRights;
    }

    /**
     * Returns a read-only collection of write right requests. The returned
     * rights are allowed to contain conflicting rights and can (though not
     * recommended) contain the same rights multiple times.
     *
     * @return the collection of write right requests. This method never
     *   returns {@code null} but may return an empty collection.
     */
    public Collection<RightType> getWriteRights() {
        return writeRights;
    }

    /**
     * Returns the string representation of this access request in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "AccessRightRequest{" + "requestID=" + requestID
                + ", readRights=" + readRights
                + ", writeRights=" + writeRights + '}';
    }
}
