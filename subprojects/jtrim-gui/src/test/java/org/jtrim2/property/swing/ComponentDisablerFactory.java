package org.jtrim2.property.swing;

import java.awt.Component;
import java.util.List;
import org.jtrim2.property.BoolPropertyListener;

public interface ComponentDisablerFactory {
    public BoolPropertyListener create(Component[] components);
    public BoolPropertyListener create(List<? extends Component> components);
}
