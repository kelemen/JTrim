package org.jtrim.swing.access;

import java.awt.Component;
import javax.swing.JPanel;
import org.jtrim.access.AccessManager;
import org.jtrim.access.HierarchicalRight;

/**
 * Defines an interface creating {@code JPanel} instances for the
 * {@link ComponentDecorator}. The {@code JPanel} instances created by this
 * {@code DecoratorPanelFactory} are to be used as a glass pane for Swing
 * components (those that have glass pane).
 * <P>
 * The {@link ComponentDecorator} needs to create {@code JPanel} instances when
 * the group of rights associated with it becomes unavailable.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface can only be used from the AWT event
 * dispatch thread (unless they allow otherwise).
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I> but due to being used on the AWT event
 * dispatch thread they must avoid expensive actions.
 *
 * @see ComponentDecorator
 * @see InvisiblePanelFactory
 *
 * @author Kelemen Attila
 */
public interface DecoratorPanelFactory {
    /**
     * Creates a new {@code JPanel} instance which is to be set as the glass
     * pane of the specified component.
     *
     * @param decorated the component whose glass pane is to be set to the
     *   returned {@code JPanel}. This argument cannot be {@code null}.
     * @param accessManager the {@code AccessManager} managing the rights in the
     *   group of rights associated with the calling {@link ComponentDecorator}.
     *   This argument cannot be {@code null}.
     * @return the new {@code JPanel} instance which is to be set as the glass
     *   pane of the specified component. This method never returns
     *   {@code null}.
     */
    public JPanel createPanel(
            Component decorated,
            AccessManager<?, HierarchicalRight> accessManager);
}
