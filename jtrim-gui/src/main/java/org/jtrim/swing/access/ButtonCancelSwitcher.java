package org.jtrim.swing.access;

import javax.swing.JButton;
import org.jtrim.access.AccessChangeAction;
import org.jtrim.access.AccessManager;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link AccessChangeAction} which sets the caption of a
 * {@code JButton} depending on the availability of the associated group of
 * rights.
 * <P>
 * The intended use of the {@code ButtonCancelSwitcher} is to set the cancel
 * caption for the button when the given rights are not available (assuming that
 * they are not available because of a task which maybe canceled).
 * <P>
 * Note that {@code ButtonCancelSwitcher} does call the {@code setText} method
 * of the {@code JButton} instances in the
 * {@link #onChangeAccess(AccessManager, boolean) onChangeAccess} method, so the
 * {@link AccessManager} governing the rights must be set to use an executor
 * which submits tasks to the AWT event dispatch thread (or wrap the
 * {@code ComponentDisabler} in an {@code AccessChangeAction} which makes sure
 * that the {@code onChangeAccess} method does not get called on an
 * inappropriate thread).
 *
 * <h3>Thread safety</h3>
 * The {@link #onChangeAccess(AccessManager, boolean) onChangeAccess} may only
 * be called from the AWT event dispatch thread but other methods are safe to
 * be accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ButtonCancelSwitcher implements AccessChangeAction {
    private final String caption;
    private final String cancelCaption;
    private final JButton button;

    /**
     * Creates a new {@code ButtonCancelSwitcher} using the specified button
     * and caption for the state when the rights are not available. When the
     * rights are available, the current caption of the button is used.
     * <P>
     * <B>Note</B>: This constructor calls the {@code getText()} method of the
     * button and therefore can only be called from the AWT event dispatch
     * thread.
     *
     * @param button the button whose caption is to be set depending on the
     *   availability of the associated rights. This argument cannot be
     *   {@code null} and cannot have a {@code null} {@code text} property.
     * @param cancelCaption the caption to be used for the button when the
     *   associated rights are not available. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} or the {@code text} property of the specified button is
     *   {@code null}
     */
    public ButtonCancelSwitcher(JButton button, String cancelCaption) {
        this(button, button.getText(), cancelCaption);
    }

    /**
     * Creates a new {@code ButtonCancelSwitcher} using the specified button
     * and captions for the state when the rights are available and when they
     * are not.
     *
     * @param button the button whose caption is to be set depending on the
     *   availability of the associated rights. This argument cannot be
     *   {@code null}.
     * @param caption the caption to be used for the button when the
     *   associated rights are available. This argument cannot be {@code null}.
     * @param cancelCaption the caption to be used for the button when the
     *   associated rights are not available. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public ButtonCancelSwitcher(JButton button,
            String caption, String cancelCaption) {

        ExceptionHelper.checkNotNullArgument(button, "button");
        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(cancelCaption, "cancelCaption");

        this.button = button;
        this.caption = caption;
        this.cancelCaption = cancelCaption;
    }

    /**
     * Sets the {@code text} property of button according to the
     * {@code available} argument. That is, sets it to the {@code caption}
     * argument of the constructor (or the button caption for the two arguments
     * constructor) if {@code available} is {@code true}; and sets it to the
     * {@code cancelCaption} argument of the constructor if {@code available} is
     * {@code false}.
     *
     * @param accessManager this argument is ignored by this method
     * @param available the {@code boolean} value defining which caption to
     *   use as explained in the method documentation
     */
    @Override
    public void onChangeAccess(AccessManager<?, ?> accessManager, boolean available) {
        button.setText(available ? caption : cancelCaption);
    }
}
