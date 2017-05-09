package org.jtrim2.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines static utility methods to track the availability of rights of an
 * {@link AccessManager} as a {@link PropertySource} (with {@code Boolean} value).
 * <P>
 * Note that these properties are not intended for synchronization purposes but
 * they can be used to enable and disable GUI components or change their state
 * otherwise.
 *
 * @see org.jtrim2.property.swing.AutoDisplayState
 */
public final class AccessProperties {
    /**
     * Returns a property which tracks if the given {@code AccessRequest} is
     * available or not. That is, if it can be acquired or not from the
     * specified {@code AccessManager}. The returned property is not intended to
     * be used as a true synchronization utility, instead it can be used to
     * change the state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param request the {@code AccessRequest} whose availability is to be
     *   tracked. This argument cannot be {@code null}.
     * @return a property which tracks if the given read and write rights are
     *   available or not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackRequestAvailable(
            AccessManager<?, ? super RightType> accessManager,
            AccessRequest<?, ? extends RightType> request) {
        return trackRightsAvailable(accessManager, request.getReadRights(), request.getWriteRights());
    }

    /**
     * Returns a property which tracks if the given read rights are available
     * or not. That is, if they can be acquired or not from the specified
     * {@code AccessManager}. The returned property is not intended to be used
     * as a true synchronization utility, instead it can be used to change the
     * state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param readRights the rights requiring read access. This argument
     *   cannot be {@code null} (but can be an empty collection) and none of its
     *   elements is allowed to be {@code null}.
     * @return a property which tracks if the given read rights are available
     *   or not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackReadRightsAvailable(
            AccessManager<?, ? super RightType> accessManager,
            Collection<? extends RightType> readRights) {
        return trackRightsAvailable(accessManager, readRights, Collections.<RightType>emptySet());
    }

    /**
     * Returns a property which tracks if the given read right is available or
     * not. That is, if it can be acquired or not from the specified
     * {@code AccessManager}. The returned property is not intended to be used
     * as a true synchronization utility, instead it can be used to change
     * the state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param readRight the right requiring read access. This argument cannot
     *   be {@code null}
     * @return a property which tracks if the given read right is available or
     *   not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackReadRightAvailable(
            AccessManager<?, ? super RightType> accessManager,
            RightType readRight) {
        return trackRightsAvailable(accessManager,
                Collections.singleton(readRight),
                Collections.<RightType>emptySet());
    }

    /**
     * Returns a property which tracks if the given write rights are available
     * or not. That is, if they can be acquired or not from the specified
     * {@code AccessManager}. The returned property is not intended to be used
     * as a true synchronization utility, instead it can be used to change the
     * state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param writeRights the rights requiring write access. This argument
     *   cannot be {@code null} (but can be an empty collection) and none of its
     *   elements is allowed to be {@code null}.
     * @return a property which tracks if the given write rights are available
     *   or not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackWriteRightsAvailable(
            AccessManager<?, ? super RightType> accessManager,
            Collection<? extends RightType> writeRights) {
        return trackRightsAvailable(accessManager, Collections.<RightType>emptySet(), writeRights);
    }

    /**
     * Returns a property which tracks if the given write right is available or
     * not. That is, if it can be acquired or not from the specified
     * {@code AccessManager}. The returned property is not intended to be used
     * as a true synchronization utility, instead it can be used to change
     * the state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param writeRight the right requiring write access. This argument cannot
     *   be {@code null}
     * @return a property which tracks if the given write right is available or
     *   not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackWriteRightAvailable(
            AccessManager<?, ? super RightType> accessManager,
            RightType writeRight) {
        return trackRightsAvailable(accessManager,
                Collections.<RightType>emptySet(),
                Collections.singleton(writeRight));
    }

    /**
     * Returns a property which tracks if the given read and write rights
     * are available or not. That is, if they can be acquired or not from the
     * specified {@code AccessManager}. The returned property is not intended to
     * be used as a true synchronization utility, instead it can be used to
     * change the state of GUI components based on the availability of rights.
     * <P>
     * Note that listeners registered with the returned property are not
     * necessarily invoked on the Event Dispatch Thread.
     *
     * @param <RightType> the type of the rights managed by the specified
     *   {@code AccessManager}
     * @param accessManager the {@code AccessManager} managing the availability
     *   of rights. This argument cannot be {@code null}.
     * @param readRights the rights requiring read access. This argument cannot
     *   be {@code null} (but can be an empty collection) and none of its
     *   elements is allowed to be {@code null}.
     * @param writeRights the rights requiring write access. This argument
     *   cannot be {@code null} (but can be an empty collection) and none of its
     *   elements is allowed to be {@code null}.
     * @return a property which tracks if the given read and write rights are
     *   available or not. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <RightType> PropertySource<Boolean> trackRightsAvailable(
            final AccessManager<?, ? super RightType> accessManager,
            Collection<? extends RightType> readRights,
            Collection<? extends RightType> writeRights) {
        // The bridge method is created, so that we can provide a more generic
        // method call avoiding syntax errors.
        return trackRightsAvailableBridge(accessManager, readRights, writeRights);
    }

    private static <IDType, RightType> PropertySource<Boolean> trackRightsAvailableBridge(
            final AccessManager<IDType, RightType> accessManager,
            Collection<? extends RightType> readRights,
            Collection<? extends RightType> writeRights) {
        return new RightTrackerPropertySource<>(accessManager, readRights, writeRights);
    }

    private AccessProperties() {
        throw new AssertionError();
    }

    private static final class RightTrackerPropertySource<IDType, RightType> implements PropertySource<Boolean> {
        private final AccessManager<IDType, RightType> accessManager;
        private final Collection<RightType> readRights;
        private final Collection<RightType> writeRights;

        public RightTrackerPropertySource(
                AccessManager<IDType, RightType> accessManager,
                Collection<? extends RightType> readRights,
                Collection<? extends RightType> writeRights) {

            Objects.requireNonNull(accessManager, "accessManager");

            this.accessManager = accessManager;
            this.readRights = new ArrayList<>(readRights);
            this.writeRights = new ArrayList<>(writeRights);

            ExceptionHelper.checkNotNullElements(this.readRights, "readRights");
            ExceptionHelper.checkNotNullElements(this.writeRights, "writeRights");
        }

        @Override
        public Boolean getValue() {
            return accessManager.isAvailable(readRights, writeRights);
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            return accessManager.addAccessChangeListener((request, acquired) -> listener.run());
        }
    }
}
