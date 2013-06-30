package org.jtrim.property.swing;

import java.awt.Component;
import java.util.List;
import org.jtrim.property.BoolPropertyListener;

/**
 *
 * @author Kelemen Attila
 */
public interface ComponentDisablerFactory {
    public BoolPropertyListener create(Component[] components);
    public BoolPropertyListener create(List<? extends Component> components);
}
