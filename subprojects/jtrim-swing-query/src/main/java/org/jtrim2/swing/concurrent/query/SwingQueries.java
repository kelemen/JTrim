package org.jtrim2.swing.concurrent.query;

import org.jtrim2.access.AccessManager;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.ui.concurrent.query.BackgroundDataProvider;

/**
 * Defines utility methods to help with asynchronous data retrieval in <I>Swing</I> applications.
 */
public final class SwingQueries {
    /**
     * Creates a {@code BackgroundDataProvider} serving the queried data on the <I>AWT Event Dispatch Thread</I>.
     *
     * @param <IDType> the type of the request ID of the underlying access manager
     * @param <RightType> the type of the rights handled by the underlying access
     *   manager
     * @param accessManager the {@code AccessManager} from which access tokens
     *   are requested to transfer data in their context. This argument cannot
     *   be {@code null}.
     * @return a {@code BackgroundDataProvider} serving the queried data on the <I>AWT Event Dispatch Thread</I>.
     *   This method never returns {@code null}.
     */
    public static <IDType, RightType> BackgroundDataProvider<IDType, RightType> getSwingBackgroundDataProvider(
            AccessManager<IDType, RightType> accessManager) {
        return new BackgroundDataProvider<>(accessManager, SwingExecutors.getSwingExecutorProvider());
    }

    private SwingQueries() {
        throw new AssertionError();
    }
}
