package org.jtrim2.concurrent.async;

import java.util.Objects;

/**
 * An {@code AsyncDataController} implementation which delegates all of its
 * methods to another {@code AsyncDataController} specified at construction
 * time.
 * <P>
 * This implementation does not declare any methods other than the ones
 * {@code AsyncDataController} offers but implements all of them by forwarding
 * to another {@code AsyncDataController} implementation specified at
 * construction time.
 * <P>
 * This class was designed for two reasons:
 * <ul>
 *  <li>
 *   To allow a safer way of class inheritance, so there can be no unexpected
 *   dependencies on overridden methods. To imitate inheritance subclass
 *   {@code DelegatedAsyncDataController}: specify the
 *   {@code AsyncDataController} you want to "subclass" in the constructor and
 *   override the required methods or provide new ones.
 *  </li>
 *  <li>
 *   To hide other public methods of an {@code AsyncDataController} from external
 *   code. This way, the external code can only access methods which the
 *   {@code AsyncDataController} interface provides.
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The thread safety properties of this class entirely depend on the wrapped
 * {@code AsyncDataController} instance.
 *
 * <h4>Synchronization transparency</h4>
 * If instances of this class are <I>synchronization transparent</I> or if its
 * synchronization control can be observed by external code entirely depends on
 * the wrapped {@code AsyncDataController} instance.
 */
public class DelegatedAsyncDataController implements AsyncDataController {
    /**
     * The {@code AsyncDataController} to which the methods are forwarded.
     * This field can never be {@code null} because the constructor throws
     * {@code NullPointerException} if {@code null} was specified as the
     * {@code AsyncDataController}.
     */
    protected final AsyncDataController wrappedController;

    /**
     * Initializes the {@link #wrappedController wrappedController} field with
     * the specified argument.
     *
     * @param controller the {@code AsyncDataController} to which the methods
     *   are forwarded. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataController} is {@code null}
     */
    public DelegatedAsyncDataController(AsyncDataController controller) {
        Objects.requireNonNull(controller, "controller");

        this.wrappedController = controller;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void controlData(Object controlArg) {
        wrappedController.controlData(controlArg);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AsyncDataState getDataState() {
        return wrappedController.getDataState();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return wrappedController.toString();
    }
}
