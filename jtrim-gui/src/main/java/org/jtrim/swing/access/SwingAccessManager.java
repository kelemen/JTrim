/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import java.awt.Component;
import java.util.*;
import java.util.concurrent.ExecutorService;
import org.jtrim.access.*;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.swing.concurrent.SwingTaskExecutor;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingAccessManager<IDType>
implements
        AccessManager<IDType, SwingRight> {
    // Lock order: mainLock, dispatcherLock
    private final HierarchicalAccessManager<IDType> accessManager;

    public static <IDType> AccessRequest<IDType, SwingRight> getRightRequest(
            IDType requestID, SwingRight[] readRights, SwingRight[] writeRights) {

        return new AccessRequest<>(requestID, readRights, writeRights);
    }

    public static <IDType> AccessRequest<IDType, SwingRight> getRightRequest(
            IDType requestID,
            Collection<? extends SwingRight> readRights,
            Collection<? extends SwingRight> writeRights) {

        return new AccessRequest<>(requestID, readRights, writeRights);
    }

    public static SwingRight[] getComponentRights(Component... components) {
        SwingRight[] rights = new SwingRight[components.length];
        for (int i = 0; i < rights.length; i++) {
            rights[i] = new SwingRight(components[i]);
        }

        return rights;
    }

    public static <IDType> AccessRequest<IDType, SwingRight> createReadRequest(
            IDType requestID, SwingRight... rights) {

        return getRightRequest(requestID, rights, null);
    }

    public static <IDType> AccessRequest<IDType, SwingRight> createReadRequest(
            IDType requestID, Component... components) {

        return createReadRequest(requestID, getComponentRights(components));
    }

    public static <IDType> AccessRequest<IDType, SwingRight> createWriteRequest(
            IDType requestID, SwingRight... rights) {

        return getRightRequest(requestID, null, rights);
    }

    public static <IDType> AccessRequest<IDType, SwingRight> createWriteRequest(
            IDType requestID, Component... components) {

        return createWriteRequest(requestID, getComponentRights(components));
    }

    public SwingAccessManager(AccessStateListener<SwingRight> accessListener) {
        this.accessManager = new HierarchicalAccessManager<>(
                SwingTaskExecutor.getStrictExecutor(false),
                SwingTaskExecutor.getStrictExecutor(false),
                accessListener != null
                    ? new StateListenerConverter(this, accessListener)
                    : null);
    }

    @Override
    public Collection<AccessToken<IDType>> getBlockingTokens(Collection<? extends SwingRight> requestedReadRights, Collection<? extends SwingRight> requestedWriteRights) {
        return accessManager.getBlockingTokens(
                toHierarchicalCollection(requestedReadRights),
                toHierarchicalCollection(requestedWriteRights));
    }

    @Override
    public boolean isAvailable(Collection<? extends SwingRight> requestedReadRights, Collection<? extends SwingRight> requestedWriteRights) {
        return accessManager.isAvailable(
                toHierarchicalCollection(requestedReadRights),
                toHierarchicalCollection(requestedWriteRights));
    }

    @Override
    public AccessResult<IDType> getScheduledAccess(
            AccessRequest<? extends IDType, ? extends SwingRight> request) {
        return accessManager.getScheduledAccess(toHierarchicalRequest(request));
    }

    @Override
    public AccessResult<IDType> getScheduledAccess(
            ExecutorService executor,
            AccessRequest<? extends IDType, ? extends SwingRight> request) {
        return accessManager.getScheduledAccess(
                executor, toHierarchicalRequest(request));
    }

    @Override
    public AccessResult<IDType> tryGetAccess(
            AccessRequest<? extends IDType, ? extends SwingRight> request) {
        return accessManager.tryGetAccess(toHierarchicalRequest(request));
    }

    @Override
    public AccessResult<IDType> tryGetAccess(
            ExecutorService executor,
            AccessRequest<? extends IDType, ? extends SwingRight> request) {
        return accessManager.tryGetAccess(
                executor, toHierarchicalRequest(request));
    }

    public boolean freeComponents(Component... components) {
        HierarchicalRight[] rights = new HierarchicalRight[components.length];
        for (int i = 0; i < rights.length; i++) {
            rights[i] = HierarchicalRight.create(components[i]);
        }

        Collection<AccessToken<IDType>> blockingTokens;

        blockingTokens = accessManager.getBlockingTokens(
                Collections.<HierarchicalRight>emptySet(),
                Arrays.asList(rights)
                );

        if (!blockingTokens.isEmpty()) {
            ExecutorsEx.shutdownExecutorsNow(blockingTokens);
            return true;
        }
        else {
            return false;
        }
    }

    public AccessResult<IDType> tryGetReadAccess(IDType requestID, SwingRight... rights) {
        return tryGetAccess(createReadRequest(requestID, rights));
    }

    public AccessResult<IDType> tryGetWriteAccess(IDType requestID, SwingRight... rights) {
        return tryGetAccess(createWriteRequest(requestID, rights));
    }

    public AccessResult<IDType> tryGetReadAccess(IDType requestID, Component... components) {
        return tryGetAccess(createReadRequest(requestID, components));
    }

    public AccessResult<IDType> tryGetWriteAccess(IDType requestID, Component... components) {
        return tryGetAccess(createWriteRequest(requestID, components));
    }

    public void getRights(
            Collection<SwingRight> readRights,
            Collection<SwingRight> writeRights) {

        List<HierarchicalRight> readHRights = new LinkedList<>();
        List<HierarchicalRight> writeHRights = new LinkedList<>();
        accessManager.getRights(readHRights, writeHRights);

        for (HierarchicalRight right: readHRights) {
            readRights.add(toSwingRight(right));
        }

        for (HierarchicalRight right: writeHRights) {
            writeRights.add(toSwingRight(right));
        }
    }

    @Override
    public String toString() {
        List<SwingRight> readRights = new LinkedList<>();
        List<SwingRight> writeRights = new LinkedList<>();
        getRights(readRights, writeRights);

        return "SwingAccessManager{"
                + "read rights=" + readRights
                + ", write rights=" + writeRights + '}';
    }

    private static SwingRight toSwingRight(HierarchicalRight right) {
        List<Object> rightList = right.getRights();
        Component component = (Component)rightList.get(0);
        Object subRight = rightList.size() > 1 ? rightList.get(1) : null;

        return new SwingRight(component, subRight);
    }

    private static HierarchicalRight toHierarchical(SwingRight right) {
        Component component = right.getComponent();
        Object subRight = right.getSubRight();

        return subRight != null
                ? HierarchicalRight.create(component, subRight)
                : HierarchicalRight.create(component);
    }

    private static <IDType> AccessRequest<IDType, HierarchicalRight> toHierarchicalRequest(
            AccessRequest<? extends IDType, ? extends SwingRight> request) {

        return new AccessRequest<>(
                request.getRequestID(),
                toHierarchicalArray(request.getReadRights()),
                toHierarchicalArray(request.getWriteRights()));
    }

    private static Collection<HierarchicalRight> toHierarchicalCollection(Collection<? extends SwingRight> rights) {
        if (rights == null) {
            return null;
        }

        Collection<HierarchicalRight> result = new ArrayList<>(rights.size());

        for (SwingRight right: rights) {
            result.add(toHierarchical(right));
        }

        return result;
    }

    private static HierarchicalRight[] toHierarchicalArray(Collection<? extends SwingRight> rights) {
        if (rights == null) {
            return null;
        }

        HierarchicalRight[] result = new HierarchicalRight[rights.size()];

        int index = 0;
        for (SwingRight right: rights) {
            result[index] = toHierarchical(right);
            index++;
        }

        return result;
    }

    private static class StateListenerConverter
            implements AccessStateListener<HierarchicalRight> {

        private final SwingAccessManager<?> swingManager;
        private final AccessStateListener<SwingRight> listener;

        public StateListenerConverter(
                SwingAccessManager<?> swingManager,
                AccessStateListener<SwingRight> listener) {
            this.swingManager = swingManager;
            this.listener = listener;
        }

        @Override
        public void onEnterState(
                AccessManager<?, HierarchicalRight> accessManager,
                HierarchicalRight right, AccessState state) {
            listener.onEnterState(swingManager, toSwingRight(right), state);
        }
    }
}
