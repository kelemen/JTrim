package org.jtrim.property.swing;

import javax.swing.JButton;
import org.jtrim.property.BoolPropertyListener;

/**
 *
 * @author Kelemen Attila
 */
public interface ButtonTextSwitcherFactory {
    public BoolPropertyListener create(JButton button, String textWhenTrue, String textWhenFalse);
    public BoolPropertyListener create(JButton button, String textWhenFalse);
}
