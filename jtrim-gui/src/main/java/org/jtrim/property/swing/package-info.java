/**
 * Contains methods and classes for manipulating and accessing <I>Swing</I>
 * components as <I>JTrim</I> {@link org.jtrim.property.PropertySource properties}.
 * <P>
 * To convert a <I>Swing</I> property to a <I>JTrim</I> property, you should
 * see the {@link org.jtrim.property.swing.SwingProperties} class for various
 * factory methods.
 *
 * <h3>Automatic disabling and enabling of <I>Swing</I> components.</h3>
 *
 * One of the main feature of this package is that you can automatically
 * change the state of a component based on boolean properties (i.e,
 * {@code PropertySource} instances having a {@code Boolean} value). The state
 * change can be the usual enable/disable component ({@code setEnabled}) but
 * this package gives you many more options. For example, you might
 * {@link org.jtrim.property.swing.AutoDisplayState#glassPaneSwitcher(javax.swing.JLayer, org.jtrim.property.swing.GlassPaneFactory)
 * change the glass pane of a JLayer}. For further options see the
 * {@link org.jtrim.property.swing.AutoDisplayState} class for factory methods.
 * <P>
 * For complicated conditions to change state, you might want to use the
 * features provided by {@link org.jtrim.property.BoolProperties}.
 *
 * @see org.jtrim.property.swing.AutoDisplayState
 * @see org.jtrim.property.swing.SwingProperties
 */
package org.jtrim.property.swing;
