package org.jtrim2.property.swing;

import javax.swing.JButton;
import org.jtrim2.property.BoolPropertyListener;

/**
 *
 * @author Kelemen Attila
 */
public interface ButtonTextSwitcherFactory {
    public BoolPropertyListener create(JButton button, String textWhenTrue, String textWhenFalse);
    public BoolPropertyListener create(JButton button, String textWhenFalse);
}
