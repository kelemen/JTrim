package org.jtrim2.ui.concurrent.query;

/**
 * Creates new independent {@link AsyncRenderer} instances. The
 * {@code AsyncRenderer} instances must be independent in a way that no two
 * {@code AsyncRenderer} instances will overwrite each other's rendering
 * requests.
 * <P>
 * For further detail on asynchronous rendering see the documentation of
 * {@link AsyncRenderer}.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safe to be accessed
 * from multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see AsyncRenderer
 * @see GenericAsyncRendererFactory
 */
public interface AsyncRendererFactory {
    /**
     * Creates an {@code AsyncRenderer} which is able to render a component.
     * <P>
     * Note that it is not strictly required that this method return a new
     * unique instance but to avoid independent {@code AsyncRenderer}
     * overwriting each other's rendering requests, it is usually necessary.
     *
     * @return an {@code AsyncRenderer} which is able to render a component.
     *   This method never returns {@code null}.
     */
    public AsyncRenderer createRenderer();
}
